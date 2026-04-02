package com.icthh.xm.commons.lep.groovy;

import java.util.Iterator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.control.SourceUnit;

import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;

@Slf4j
public class GroovyFileParser {

    @SneakyThrows
    public GroovyFileMetadata getFileMetaData(String filePath, String source) {
        try {
            return getGroovyFileMetadata(filePath, source);
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
