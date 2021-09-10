package com.icthh.xm.commons.lep;

import groovyjarjarantlr.RecognitionException;
import groovyjarjarantlr.TokenStreamException;
import groovyjarjarantlr.collections.AST;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableBoolean;
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

import java.io.IOException;
import java.io.StringReader;

@Slf4j
public class GroovyFileParser {

    @SneakyThrows
    public boolean isFileContainsClassDefinition(String source, String className) {
        try {
            MutableBoolean isExistsDef = new MutableBoolean(false);
            parseGroovy(source, new VisitorAdapter() {
                @Override
                public void visitClassDef(GroovySourceAST t, int visit) {
                    visitDef(t, className, isExistsDef);
                }

                @Override
                public void visitInterfaceDef(GroovySourceAST t, int visit) {
                    visitDef(t, className, isExistsDef);
                }

                @Override
                public void visitEnumDef(GroovySourceAST t, int visit) {
                    visitDef(t, className, isExistsDef);
                }

                @Override
                public void visitAnnotationDef(GroovySourceAST t, int visit) {
                    visitDef(t, className, isExistsDef);
                }

                private void visitDef(GroovySourceAST t, String className, MutableBoolean isExistsDef) {
                    boolean isClassDefPresent = className.equals(t.childOfType(GroovyTokenTypes.IDENT).getText());
                    isExistsDef.setValue(isExistsDef.booleanValue() || isClassDefPresent);
                }
            });
            return isExistsDef.getValue();
        } catch (Exception e) {
            log.error("Error parse groovy", e);
        }
        return false;
    }

    public void parseGroovy(String source, Visitor visitor) throws TokenStreamException, RecognitionException, IOException {
        SourceBuffer sourceBuffer = new SourceBuffer();
        GroovyRecognizer parser = getGroovyParser(source, sourceBuffer);
        parser.compilationUnit();
        AST ast = parser.getAST();
        AntlrASTProcessor traverser = new SourceCodeTraversal(visitor);
        traverser.process(ast);
    }

    private static GroovyRecognizer getGroovyParser(String input, SourceBuffer sourceBuffer) throws IOException {
        UnicodeEscapingReader unicodeReader = new UnicodeEscapingReader(new StringReader(input), sourceBuffer);
        GroovyLexer lexer = new GroovyLexer(unicodeReader);
        unicodeReader.setLexer(lexer);
        GroovyRecognizer parser = GroovyRecognizer.make(lexer);
        parser.setSourceBuffer(sourceBuffer);
        return parser;
    }
}
