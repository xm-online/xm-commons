package com.icthh.xm.commons.lep.groovy;

import groovyjarjarantlr4.v4.runtime.CharStreams;
import groovyjarjarantlr4.v4.runtime.CommonTokenStream;
import groovyjarjarantlr4.v4.runtime.ParserRuleContext;
import groovyjarjarantlr4.v4.runtime.tree.ParseTreeWalker;
import groovyjarjarantlr4.v4.runtime.tree.ParseTreeListener;
import groovyjarjarantlr4.v4.runtime.tree.ErrorNode;
import groovyjarjarantlr4.v4.runtime.tree.TerminalNode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.groovy.parser.antlr4.GroovyLexer;
import org.apache.groovy.parser.antlr4.GroovyParser;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

@Slf4j
public class GroovyFileParser {

    @SneakyThrows
    public GroovyFileMetadata getFileMetaData(String source) {
        try {
            return getGroovyFileMetadata(source);
        } catch (Exception e) {
            log.error("Error parsing groovy: {}", e.getMessage(), e);
        }
        return new GroovyFileMetadata();
    }

    public GroovyFileMetadata getGroovyFileMetadata(String source) {
        GroovyFileMetadata metadata = new GroovyFileMetadata();
        parseGroovy(source, new GroovyParseListener(metadata));
        return metadata;
    }

    private void parseGroovy(String source, GroovyParseListener listener) {
        GroovyLexer lexer = new GroovyLexer(CharStreams.fromString(source));
        GroovyParser parser = new GroovyParser(new CommonTokenStream(lexer));
        ParseTreeWalker.DEFAULT.walk(listener, parser.compilationUnit());
    }

    @ToString
    @Getter
    @RequiredArgsConstructor
    public static class GroovyFileMetadata {
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

    private static class GroovyParseListener implements ParseTreeListener {
        private final LinkedList<String> classNames = new LinkedList<>();
        private final GroovyFileMetadata metadata;
        private boolean inAnnotation = false;

        public GroovyParseListener(GroovyFileMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void visitTerminal(TerminalNode node) {
            // Not used
        }

        @Override
        public void visitErrorNode(ErrorNode node) {
            // Not used
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            if (ctx instanceof GroovyParser.AnnotationContext) {
                inAnnotation = true;
            } else if (ctx instanceof GroovyParser.ClassDeclarationContext classCtx) {
                String className = classCtx.identifier().getText();
                classNames.addLast(className);
                metadata.getClasses().add(String.join("$", classNames));
            } else if (ctx instanceof GroovyParser.FieldDeclarationContext fieldCtx) {
                if (classNames.isEmpty() && !inAnnotation) {
                    metadata.isScript = true;
                } else if (!classNames.isEmpty() && fieldCtx.getParent() != null && fieldCtx.getParent().getText().contains("static")) {
                    String fieldName = fieldCtx.variableDeclaration().variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText();
                    metadata.getStaticFields().add(getCurrentClassName() + "." + fieldName);
                }
            } else if (ctx instanceof GroovyParser.MethodDeclarationContext methodCtx) {
                if (classNames.isEmpty() && !inAnnotation) {
                    metadata.isScript = true;
                } else if (!classNames.isEmpty() && methodCtx.getParent() != null && methodCtx.getParent().getText().contains("static")) {
                    String methodName = methodCtx.methodName().identifier().getText();
                    metadata.getStaticMethods().add(getCurrentClassName() + "." + methodName);
                }
            } else if (ctx instanceof GroovyParser.EnumConstantContext enumCtx) {
                String constantName = enumCtx.identifier().getText();
                metadata.getStaticFields().add(getCurrentClassName() + "." + constantName);
            }
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            if (ctx instanceof GroovyParser.AnnotationContext) {
                inAnnotation = false;
            } else if (ctx instanceof GroovyParser.ClassDeclarationContext) {
                classNames.removeLast();
            }
        }

        private String getCurrentClassName() {
            return String.join("$", classNames);
        }
    }
}
