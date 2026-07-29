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
        GroovyFileParser gfp = new GroovyFileParser(GroovyFileParser.DEFAULT_METADATA_CACHE_MAX_SIZE) {
            @Override
            public GroovyFileMetadata getGroovyFileMetadata(String filePath, String source) {
                parseCount.incrementAndGet();
                return super.getGroovyFileMetadata(filePath, source);
            }
        };

        String source = "package TEST.testApp.lep.commons\n"
            + "class CachedClass {\n static def field = 1\n static def method() {}\n}\n";

        gfp.getFileMetaData("engine", "lep/CachedClass.groovy", source);
        GroovyFileParser.GroovyFileMetadata second = gfp.getFileMetaData("engine", "lep/other/CachedClass.groovy", source);

        assertEquals(1, parseCount.get());
        assertTrue(second.canImport("CachedClass"));
        assertTrue(second.canImport("CachedClass.field"));
        assertTrue(second.canImport("CachedClass.method"));

        String changed = source.replace("CachedClass", "ChangedClass");
        GroovyFileParser.GroovyFileMetadata third = gfp.getFileMetaData("engine", "lep/CachedClass.groovy", changed);
        assertEquals(2, parseCount.get());
        assertTrue(third.canImport("ChangedClass"));
    }

    @Test
    public void testMetadataKeptWhileAnyEngineHoldsIt() {
        AtomicInteger parseCount = new AtomicInteger();
        GroovyFileParser gfp = countingParser(parseCount);

        // both engines use 'shared', only the first one uses 'onlyInFirst'
        gfp.getFileMetaData("engine-1", "shared.groovy", "return 'shared'");
        gfp.getFileMetaData("engine-1", "only.groovy", "return 'onlyInFirst'");
        gfp.getFileMetaData("engine-2", "shared.groovy", "return 'shared'");
        assertEquals(2, parseCount.get());

        gfp.releaseEngine("engine-1");

        // 'shared' is still held by engine-2
        gfp.getFileMetaData("engine-2", "shared.groovy", "return 'shared'");
        assertEquals(2, parseCount.get());

        // 'onlyInFirst' lost its last engine and was dropped
        gfp.getFileMetaData("engine-2", "only.groovy", "return 'onlyInFirst'");
        assertEquals(3, parseCount.get());
    }

    @Test
    public void testMetadataDroppedWhenLastEngineDestroyed() {
        AtomicInteger parseCount = new AtomicInteger();
        GroovyFileParser gfp = countingParser(parseCount);

        gfp.getFileMetaData("engine-1", "lep.groovy", "return 'lep'");
        gfp.getFileMetaData("engine-2", "lep.groovy", "return 'lep'");
        assertEquals(1, parseCount.get());

        gfp.releaseEngine("engine-1");
        gfp.releaseEngine("engine-2");

        gfp.getFileMetaData("engine-3", "lep.groovy", "return 'lep'");
        assertEquals(2, parseCount.get());
    }

    @Test
    public void testReleaseOfUnknownEngineChangesNothing() {
        AtomicInteger parseCount = new AtomicInteger();
        GroovyFileParser gfp = countingParser(parseCount);

        gfp.getFileMetaData("engine-1", "lep.groovy", "return 'lep'");
        gfp.releaseEngine("never-created");
        gfp.releaseEngine("engine-1");
        gfp.releaseEngine("engine-1");

        gfp.getFileMetaData("engine-1", "lep.groovy", "return 'lep'");
        assertEquals(2, parseCount.get());
    }

    private static GroovyFileParser countingParser(AtomicInteger parseCount) {
        return new GroovyFileParser(GroovyFileParser.DEFAULT_METADATA_CACHE_MAX_SIZE) {
            @Override
            public GroovyFileMetadata getGroovyFileMetadata(String filePath, String source) {
                parseCount.incrementAndGet();
                return super.getGroovyFileMetadata(filePath, source);
            }
        };
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

        gfp.getFileMetaData("engine", "a.groovy", "return 'a'");
        gfp.getFileMetaData("engine", "b.groovy", "return 'b'");
        gfp.getFileMetaData("engine", "c.groovy", "return 'c'");
        assertEquals(3, parseCount.get());

        // 'a' was evicted (cache size 2), so it must be parsed again
        gfp.getFileMetaData("engine", "a.groovy", "return 'a'");
        assertEquals(4, parseCount.get());

        // 'c' is still cached
        gfp.getFileMetaData("engine", "c.groovy", "return 'c'");
        assertEquals(4, parseCount.get());
    }
}
