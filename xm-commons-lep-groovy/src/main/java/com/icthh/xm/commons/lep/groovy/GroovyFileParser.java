package com.icthh.xm.commons.lep.groovy;

import groovyjarjarantlr4.v4.runtime.CharStreams;
import groovyjarjarantlr4.v4.runtime.CommonTokenStream;
import groovyjarjarantlr4.v4.runtime.tree.ParseTreeListener;
import groovyjarjarantlr4.v4.runtime.tree.ParseTreeWalker;
import groovyjarjarantlr4.v4.runtime.tree.xpath.XPathLexer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Parser;
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
        XPathLexer tokenSource = new XPathLexer(CharStreams.fromString(source));
        GroovyParser parser = new GroovyParser(new CommonTokenStream(tokenSource));
        ParseTreeWalker.DEFAULT.walk((ParseTreeListener) listener, parser.compilationUnit());
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

    private static class GroovyParseListener extends Parser.TrimToSizeListener {
        private final LinkedList<String> classNames = new LinkedList<>();
        private final GroovyFileMetadata metadata;

        public GroovyParseListener(GroovyFileMetadata metadata) {
            this.metadata = metadata;
        }

        public void enterClassDeclaration(GroovyParser.ClassDeclarationContext ctx) {
            String className = ctx.identifier().getText();
            classNames.addLast(className);
            metadata.getClasses().add(String.join("$", classNames));
        }

        public void exitClassDeclaration(GroovyParser.ClassDeclarationContext ctx) {
            classNames.removeLast();
        }

        public void enterFieldDeclaration(GroovyParser.FieldDeclarationContext ctx) {
            if (ctx.getParent() != null && ctx.getParent().getText().contains("static")) {
                String fieldName = ctx.variableDeclaration().variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText();
                metadata.getStaticFields().add(getCurrentClassName() + "." + fieldName);
            }
        }

        public void enterMethodDeclaration(GroovyParser.MethodDeclarationContext ctx) {
            if (ctx.getParent() != null && ctx.getParent().getText().contains("static")) {
                String methodName = ctx.methodName().identifier().getText();
                metadata.getStaticMethods().add(getCurrentClassName() + "." + methodName);
            }
        }

        public void enterEnumConstant(GroovyParser.EnumConstantContext ctx) {
            String constantName = ctx.identifier().getText();
            metadata.getStaticFields().add(getCurrentClassName() + "." + constantName);
        }

        private String getCurrentClassName() {
            return String.join("$", classNames);
        }
    }
}
