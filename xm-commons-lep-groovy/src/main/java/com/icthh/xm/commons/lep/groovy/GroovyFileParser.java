package com.icthh.xm.commons.lep.groovy;

import groovyjarjarantlr.RecognitionException;
import groovyjarjarantlr.TokenStreamException;
import groovyjarjarantlr.collections.AST;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.antlr.AntlrASTProcessor;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.codehaus.groovy.antlr.treewalker.SourceCodeTraversal;
import org.codehaus.groovy.antlr.treewalker.Visitor;
import org.codehaus.groovy.antlr.treewalker.VisitorAdapter;

import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.IDENT;
import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.LITERAL_static;
import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.MODIFIERS;

@Slf4j
public class GroovyFileParser {

    @SneakyThrows
    public GroovyFileMetadata getFileMetaData(String source) {

        try {
            GroovyFileMetadata metadata = new GroovyFileMetadata();
            parseGroovy(source, new GroovyParseVisitor(metadata));
            return metadata;
        } catch (Throwable e) {
            log.error("Error parse groovy: {}", e.getMessage(), e);
        }


        return new GroovyFileMetadata();
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

    private static class GroovyParseVisitor extends VisitorAdapter {
        private final LinkedList<String> classNames;
        private final LinkedList<Integer> levels;
        private final GroovyFileMetadata metadata;
        private int level;

        private boolean isInAnnotation;
        private boolean isInAnnotationArray;

        public GroovyParseVisitor(GroovyFileMetadata metadata) {
            this.metadata = metadata;
            classNames = new LinkedList<>();
            levels = new LinkedList<>();
            level = 0;
            isInAnnotation = false;
            isInAnnotationArray = false;
        }

        @Override
        public void visitVariableDef(GroovySourceAST t, int visit) {
            if (isStatic(t)) {
                metadata.staticFields.add(StringUtils.join(classNames, "$") + "." + getIdent(t));
            }
            super.visitVariableDef(t, visit); // to detect outside code
        }

        @Override
        public void visitMethodDef(GroovySourceAST t, int visit) {
            if (isStatic(t)) {
                metadata.staticMethods.add(StringUtils.join(classNames, "$") + "." + getIdent(t));
            }
            super.visitMethodDef(t, visit); // to detect outside code
        }

        @Override
        public void visitEnumConstantDef(GroovySourceAST t, int visit) {
            metadata.staticFields.add(StringUtils.join(classNames, "$") + "." + getIdent(t));
        }

        @Override
        public void visitClassDef(GroovySourceAST t, int visit) {
            if (visit != OPENING_VISIT) {
                return;
            }

            String currentClassName = getIdent(t);
            classNames.addLast(currentClassName);
            levels.addLast(level);
            level = 0;
            metadata.classes.add(StringUtils.join(classNames, "$"));
        }

        @Override
        public void visitObjblock(GroovySourceAST t, int visit) {
            if (visit == OPENING_VISIT) {
                level++;
            } else if (visit == CLOSING_VISIT) {
                level--;
                if (level == 0) {
                    classNames.removeLast();
                    levels.removeLast();

                    if (!levels.isEmpty()) {
                        level = levels.getLast();
                    }
                }
            }
        }

        @Override
        public void visitInterfaceDef(GroovySourceAST t, int visit) {
            visitClassDef(t, visit);
        }

        @Override
        public void visitEnumDef(GroovySourceAST t, int visit) {
            visitClassDef(t, visit);
        }

        @Override
        public void visitAnnotationDef(GroovySourceAST t, int visit) {
            visitClassDef(t, visit);
        }

        @Override
        public void visitPackageDef(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitStaticImport(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitImport(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitMlComment(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitShComment(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitSlComment(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitAnnotation(GroovySourceAST t, int visit) {
            // it's not are outside code
            if (visit == OPENING_VISIT) {
                isInAnnotation = true;
            } else if (visit == CLOSING_VISIT) {
                isInAnnotation = false;
            }
        }

        @Override
        public void visitAnnotations(GroovySourceAST t, int visit) {
            // it's not are outside code
            if (visit == OPENING_VISIT) {
                isInAnnotationArray = true;
            } else if (visit == CLOSING_VISIT) {
                isInAnnotationArray = false;
            }
        }

        @Override
        public void visitIdent(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitDot(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitModifiers(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitExtendsClause(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitImplementsClause(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitAnnotationMemberValuePair(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitAnnotationArrayInit(GroovySourceAST t, int visit) {
            // it's not are outside code
        }

        @Override
        public void visitDefault(GroovySourceAST t, int visit) {
            if (level == 0 && !isInAnnotation && !isInAnnotationArray) {
                super.visitDefault(t, visit);
                metadata.isScript = true;
            }
        }

        private boolean isStatic(GroovySourceAST t) {
            return Optional.ofNullable(t.childOfType(MODIFIERS))
                .map(it -> it.childOfType(LITERAL_static)).isPresent();
        }

        private String getIdent(GroovySourceAST t) {
            return t.childOfType(IDENT).getText();
        }

    }
}
