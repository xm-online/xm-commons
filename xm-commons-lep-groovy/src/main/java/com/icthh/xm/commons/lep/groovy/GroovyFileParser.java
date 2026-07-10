package com.icthh.xm.commons.lep.groovy;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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

@Slf4j
public class GroovyFileParser {

    private static final int METADATA_CACHE_MAX_SIZE = 10_000;

    // metadata depends only on the source text, so unchanged files skip the AST parse on engine refresh
    private final Map<String, GroovyFileMetadata> metadataByContentHash;

    public GroovyFileParser() {
        this(METADATA_CACHE_MAX_SIZE);
    }

    public GroovyFileParser(int metadataCacheMaxSize) {
        this.metadataByContentHash = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, GroovyFileMetadata> eldest) {
                    return size() > metadataCacheMaxSize;
                }
            });
    }

    @SneakyThrows
    public GroovyFileMetadata getFileMetaData(String filePath, String source) {
        try {
            String contentHash = DigestUtils.sha256Hex(source);
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

    public GroovyFileMetadata getGroovyFileMetadata(String filePath, String source) {
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
