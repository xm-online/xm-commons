package com.icthh.xm.commons.lep.groovy;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.control.SourceUnit;

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;

import static java.lang.Boolean.TRUE;

@Slf4j
public class GroovyFileParser {

    /**
     * Default upper bound of {@code application.lep.file-metadata-cache-size}. The real cardinality is the
     * number of distinct lep sources of all tenants, so this bound is only a runaway guard, not a working size.
     */
    public static final int DEFAULT_METADATA_CACHE_MAX_SIZE = 10_000_000;

    @Getter
    private final int metadataCacheMaxSize;

    /** EXPERIMENTAL, off by default. See {@code application.lep.experimental-metadata-prescan}. */
    @Getter
    private final boolean experimentalPrescanEnabled;

    // metadata depends only on the source text, so unchanged files skip the AST parse on engine refresh
    private final Map<String, GroovyFileMetadata> metadataByContentHash;

    // the lep engines that asked for each cached source, so an entry lives exactly as long as an engine holds it
    private final Map<String, Map<String, Boolean>> engineIdsByContentHash = new ConcurrentHashMap<>();

    public GroovyFileParser(int metadataCacheMaxSize) {
        this(metadataCacheMaxSize, false);
    }

    public GroovyFileParser(int metadataCacheMaxSize, boolean experimentalPrescanEnabled) {
        this.metadataCacheMaxSize = metadataCacheMaxSize;
        this.experimentalPrescanEnabled = experimentalPrescanEnabled;
        this.metadataByContentHash = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, GroovyFileMetadata> eldest) {
                    return size() > metadataCacheMaxSize;
                }
            });
    }

    @SneakyThrows
    public GroovyFileMetadata getFileMetaData(String engineId, String filePath, String source) {
        try {
            String contentHash = DigestUtils.sha256Hex(source);
            // the owner is registered before the entry is cached, so a concurrent release of another
            // engine cannot drop what is being put here
            engineIdsByContentHash
                .computeIfAbsent(contentHash, it -> new ConcurrentHashMap<>())
                .putIfAbsent(engineId, TRUE);

            GroovyFileMetadata cached = metadataByContentHash.get(contentHash);
            if (cached != null) {
                return cached;
            }
            GroovyFileMetadata metadata = getGroovyFileMetadata(filePath, source);
            metadataByContentHash.put(contentHash, metadata);
            return metadata;
        } catch (Exception e) {
            log.error("Error parsing groovy source: {}", e.getMessage(), e);
            return new GroovyFileMetadata();
        }
    }

    /**
     * Drops the metadata of every source that no live lep engine holds any more. The cache map is touched
     * only for the sources actually released, which on a config refresh are the ones that changed or went
     * away - the rest are already held by the engine created for the new config.
     */
    public void releaseEngine(String engineId) {
        engineIdsByContentHash.forEach((contentHash, engineIds) -> {
            if (engineIds.remove(engineId) != null
                && engineIds.isEmpty()
                && engineIdsByContentHash.remove(contentHash, engineIds)) {
                metadataByContentHash.remove(contentHash);
            }
        });
    }

    public GroovyFileMetadata getGroovyFileMetadata(String filePath, String source) {
        if (experimentalPrescanEnabled && !GroovySourceScanner.mayDeclareType(source)) {
            // a source without any type declaration compiles to a bare script class and holds nothing
            // importable, so its metadata is already known and the AST does not have to be built
            GroovyFileMetadata metadata = new GroovyFileMetadata();
            metadata.setScript(true);
            return metadata;
        }
        return parseGroovyFileMetadata(filePath, source);
    }

    protected GroovyFileMetadata parseGroovyFileMetadata(String filePath, String source) {
        GroovyFileMetadata metadata = new GroovyFileMetadata();

        SourceUnit sourceUnit = SourceUnit.create(filePath, source);

        sourceUnit.parse();
        sourceUnit.completePhase();
        sourceUnit.convert();

        for (ClassNode classNode : sourceUnit.getAST().getClasses()) {
            if (classNode.isScript()) {
                metadata.setScript(true);
            } else {
                processClassNode(classNode, metadata);
            }
        }

        return metadata;
    }

    private void processClassNode(ClassNode classNode, GroovyFileMetadata metadata) {
        String className = classNode.getNameWithoutPackage();
        metadata.getClasses().add(className);

        for (FieldNode field : classNode.getFields()) {
            if (field.isStatic()) {
                metadata.getStaticFields().add(className + "." + field.getName());
            }
        }

        for (MethodNode method : classNode.getMethods()) {
            if (method.isStatic()) {
                metadata.getStaticMethods().add(className + "." + method.getName());
            }
        }

        for (Iterator<InnerClassNode> it = classNode.getInnerClasses(); it.hasNext(); ) {
            ClassNode inner = it.next();
            processClassNode(inner, metadata);
        }
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    public static class GroovyFileMetadata {

        @Setter
        private boolean isScript = false;
        private final Set<String> classes = new HashSet<>();
        private final Set<String> staticFields = new HashSet<>();
        private final Set<String> staticMethods = new HashSet<>();

        public boolean canImport(String importValue) {
            return classes.contains(importValue)
                    || staticFields.contains(importValue)
                    || staticMethods.contains(importValue);
        }
    }
}
