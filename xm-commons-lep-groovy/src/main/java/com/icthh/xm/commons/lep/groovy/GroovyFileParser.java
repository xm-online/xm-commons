package com.icthh.xm.commons.lep.groovy;

import groovyjarjarantlr.RecognitionException;
import groovyjarjarantlr.TokenStreamException;
import groovyjarjarantlr.collections.AST;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.antlr.AntlrASTProcessor;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import org.codehaus.groovy.antlr.treewalker.SourceCodeTraversal;
import org.codehaus.groovy.antlr.treewalker.Visitor;
import org.codehaus.groovy.antlr.treewalker.VisitorAdapter;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class GroovyFileParser {

    @SneakyThrows
    public Set<String> getFileClassDefinition(String source) {
        Set<String> classes = new HashSet<>();
        try {
            parseGroovy(source, new VisitorAdapter() {
                @Override
                public void visitClassDef(GroovySourceAST t, int visit) {
                    visitDef(t);
                }

                @Override
                public void visitInterfaceDef(GroovySourceAST t, int visit) {
                    visitDef(t);
                }

                @Override
                public void visitEnumDef(GroovySourceAST t, int visit) {
                    visitDef(t);
                }

                @Override
                public void visitAnnotationDef(GroovySourceAST t, int visit) {
                    visitDef(t);
                }

                private void visitDef(GroovySourceAST t) {
                    classes.add(t.childOfType(GroovyTokenTypes.IDENT).getText());
                }
            });
        } catch (Throwable e) {
            log.error("Error parse groovy", e);
        }
        return classes;
    }

    private void parseGroovy(String source, Visitor visitor) throws TokenStreamException, RecognitionException {
        SourceBuffer sourceBuffer = new SourceBuffer();
        GroovyRecognizer parser = getGroovyParser(source, sourceBuffer);
        parser.compilationUnit();
        AST ast = parser.getAST();
        AntlrASTProcessor traverser = new SourceCodeTraversal(visitor);
        traverser.process(ast);
    }

    private static GroovyRecognizer getGroovyParser(String input, SourceBuffer sourceBuffer) {
        UnicodeEscapingReader unicodeReader = new UnicodeEscapingReader(new StringReader(input), sourceBuffer);
        GroovyLexer lexer = new GroovyLexer(unicodeReader);
        unicodeReader.setLexer(lexer);
        GroovyRecognizer parser = GroovyRecognizer.make(lexer);
        parser.setSourceBuffer(sourceBuffer);
        return parser;
    }
}
