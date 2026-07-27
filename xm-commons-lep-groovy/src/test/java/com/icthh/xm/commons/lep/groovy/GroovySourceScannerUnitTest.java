package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.groovy.GroovyFileParser.GroovyFileMetadata;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The experimental pre-scan is only allowed to skip the AST parse when that cannot change the resulting
 * metadata. Every case here asserts the shortcut against the full parse of the same source.
 */
public class GroovySourceScannerUnitTest {

    private final GroovyFileParser parser =
        new GroovyFileParser(GroovyFileParser.DEFAULT_METADATA_CACHE_MAX_SIZE, true);

    @Test
    void shortcutMatchesFullParseForEveryTestLep() throws IOException {
        List<Path> leps = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/test/resources/lep"))) {
            files.filter(path -> path.toString().endsWith(".groovy")).forEach(leps::add);
        }

        assertFalse(leps.isEmpty(), "no lep resources found - check the test working directory");
        leps.forEach(path -> assertSameAsFullParse(readString(path), path.toString()));
    }

    @Test
    void shortcutMatchesFullParseForScripts() {
        assertSameAsFullParse("return lepContext.methodArgs.value\n", "plain script");
        assertSameAsFullParse("", "empty file");
        assertSameAsFullParse("// only a comment\n", "comment only");
        assertSameAsFullParse("/* only\n a block\n comment */\n", "block comment only");
        assertSameAsFullParse("package a.b.c\n", "package only");
        assertSameAsFullParse("package a.b.c\nimport java.time.Instant\nimport static java.util.Map.of\n", "imports only");
        assertSameAsFullParse("def helper(def a) { return a }\n", "method only");
        assertSameAsFullParse("import java.time.Instant\ndef now = Instant.now()\nreturn now\n", "import and code");
    }

    @Test
    void shortcutMatchesFullParseWhenKeywordIsNotADeclaration() {
        assertSameAsFullParse("return lepContext.getClass()\n", "getClass call");
        assertSameAsFullParse("return String.class\n", "class literal");
        assertSameAsFullParse("return String.class.name\n", "class literal with property");
        assertSameAsFullParse("def record = 2\ndef trait = 3\nreturn record + trait\n",
            "contextual keywords used as identifiers");
        assertSameAsFullParse("// class Hidden { }\nreturn 1\n", "declaration inside a line comment");
        assertSameAsFullParse("/* class Hidden { } */\nreturn 1\n", "declaration inside a block comment");
        assertSameAsFullParse("return 'class Hidden { }'\n", "declaration inside a single quoted string");
        assertSameAsFullParse("return \"class Hidden { }\"\n", "declaration inside a double quoted string");
        assertSameAsFullParse("return '''class Hidden { }'''\n", "declaration inside a triple quoted string");
        assertSameAsFullParse("def name = 'x'\nreturn \"class ${name} { }\"\n", "declaration inside a gstring");
        assertSameAsFullParse("return 'it\\'s not a class Hidden'\n", "escaped quote inside a string");
    }

    @Test
    void shortcutMatchesFullParseForDeclarations() {
        assertSameAsFullParse("package a.b\nclass Service { static def call() { return 1 } }\n", "class");
        assertSameAsFullParse("interface Marker { }\n", "interface");
        assertSameAsFullParse("@interface Marker { }\n", "annotation type");
        assertSameAsFullParse("enum Color { RED, GREEN }\n", "enum");
        assertSameAsFullParse("trait Greeter { def hello() { return 'hi' } }\n", "trait");
        assertSameAsFullParse("record Point(int x, int y) { }\n", "record");
        assertSameAsFullParse("abstract class Base { }\nfinal class Impl extends Base { }\n", "several classes");
        assertSameAsFullParse("class Outer { static class Inner { static def value = 1 } }\n", "nested class");
        assertSameAsFullParse("def r = new Runnable() { void run() { } }\nreturn r\n", "anonymous class");
        assertSameAsFullParse("return new Runnable() {\n    void run() { }\n}\n", "anonymous class over several lines");
    }

    @Test
    void skipsTheParseOnlyForSourcesWithoutAnyTypeDeclaration() {
        assertFalse(GroovySourceScanner.mayDeclareType("return String.class\n"));
        assertFalse(GroovySourceScanner.mayDeclareType("// class Hidden {}\n"));
        assertFalse(GroovySourceScanner.mayDeclareType("def m = new HashMap()\nm.each { k, v -> k }\n"));
        assertFalse(GroovySourceScanner.mayDeclareType("return new Foo(new Bar(1), 'x')\n"));
        assertTrue(GroovySourceScanner.mayDeclareType("class Service { }\n"));
        assertTrue(GroovySourceScanner.mayDeclareType("@interface Marker { }\n"));
        assertTrue(GroovySourceScanner.mayDeclareType("def r = new Runnable() { void run() { } }\n"));
    }

    /**
     * Sources that do not compile cannot be compared against the full parse - it throws instead of returning
     * metadata. What must hold for them is that the pre-scan never swallows a declaration it does not
     * understand: the source still reaches the parser, and a broken source still yields empty metadata.
     */
    @Test
    void doesNotSkipTheParseForSourcesItCannotRead() {
        assertTrue(GroovySourceScanner.mayDeclareType("class\nBroken { }\n"),
            "a newline between the keyword and the name must not hide the declaration");
        assertTrue(GroovySourceScanner.mayDeclareType("class /* here */ Broken { }\n"),
            "a comment between the keyword and the name must not hide the declaration");
        assertTrue(GroovySourceScanner.mayDeclareType("class Broken { def x = \n"),
            "an unterminated class body must not hide the declaration");
        assertTrue(GroovySourceScanner.mayDeclareType("def s = 'unterminated\nclass Broken { }\n"),
            "an unterminated string must not hide a declaration behind it");

        GroovyFileMetadata metadata = parser.getFileMetaData("lep/Broken.groovy", "class\nBroken { }\n");
        assertTrue(metadata.getClasses().isEmpty(), "a source that does not compile has no metadata");
    }

    @Test
    void parserWithoutTheFlagAlwaysBuildsTheAst() {
        GroovyFileParser withoutPrescan = new GroovyFileParser(GroovyFileParser.DEFAULT_METADATA_CACHE_MAX_SIZE);
        assertFalse(withoutPrescan.isExperimentalPrescanEnabled(), "the pre-scan must be opt in");

        String source = "return 'a plain script'\n";
        assertEquals(
            withoutPrescan.parseGroovyFileMetadata("lep/Script.groovy", source).isScript(),
            withoutPrescan.getGroovyFileMetadata("lep/Script.groovy", source).isScript());
    }

    private void assertSameAsFullParse(String source, String description) {
        GroovyFileMetadata expected = parser.parseGroovyFileMetadata("lep/Test.groovy", source);
        GroovyFileMetadata actual = parser.getGroovyFileMetadata("lep/Test.groovy", source);

        assertEquals(expected.isScript(), actual.isScript(), "isScript of [" + description + "]");
        assertEquals(expected.getClasses(), actual.getClasses(), "classes of [" + description + "]");
        assertEquals(expected.getStaticFields(), actual.getStaticFields(), "static fields of [" + description + "]");
        assertEquals(expected.getStaticMethods(), actual.getStaticMethods(), "static methods of [" + description + "]");
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
