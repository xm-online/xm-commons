package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.groovy.GroovyFileParser;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroovyFileParserCacheUnitTest {

    @Test
    public void testMetadataCachedByContentHash() {
        AtomicInteger parseCount = new AtomicInteger();
        GroovyFileParser gfp = new GroovyFileParser() {
            @Override
            public GroovyFileMetadata getGroovyFileMetadata(String filePath, String source) {
                parseCount.incrementAndGet();
                return super.getGroovyFileMetadata(filePath, source);
            }
        };

        String source = "package TEST.testApp.lep.commons\n"
            + "class CachedClass {\n static def field = 1\n static def method() {}\n}\n";

        gfp.getFileMetaData("lep/CachedClass.groovy", source);
        GroovyFileParser.GroovyFileMetadata second = gfp.getFileMetaData("lep/other/CachedClass.groovy", source);

        assertEquals(1, parseCount.get());
        assertTrue(second.canImport("CachedClass"));
        assertTrue(second.canImport("CachedClass.field"));
        assertTrue(second.canImport("CachedClass.method"));

        String changed = source.replace("CachedClass", "ChangedClass");
        GroovyFileParser.GroovyFileMetadata third = gfp.getFileMetaData("lep/CachedClass.groovy", changed);
        assertEquals(2, parseCount.get());
        assertTrue(third.canImport("ChangedClass"));
    }

    @Test
    public void testEvictsLeastRecentlyUsedEntriesWhenCacheFull() {
        AtomicInteger parseCount = new AtomicInteger();
        GroovyFileParser gfp = new GroovyFileParser(2) {
            @Override
            public GroovyFileMetadata getGroovyFileMetadata(String filePath, String source) {
                parseCount.incrementAndGet();
                return super.getGroovyFileMetadata(filePath, source);
            }
        };

        gfp.getFileMetaData("a.groovy", "return 'a'");
        gfp.getFileMetaData("b.groovy", "return 'b'");
        gfp.getFileMetaData("c.groovy", "return 'c'");
        assertEquals(3, parseCount.get());

        // 'a' was evicted (cache size 2), so it must be parsed again
        gfp.getFileMetaData("a.groovy", "return 'a'");
        assertEquals(4, parseCount.get());

        // 'c' is still cached
        gfp.getFileMetaData("c.groovy", "return 'c'");
        assertEquals(4, parseCount.get());
    }
}
