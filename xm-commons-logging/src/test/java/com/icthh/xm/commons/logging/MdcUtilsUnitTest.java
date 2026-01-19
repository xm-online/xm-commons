package com.icthh.xm.commons.logging;

import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for MdcUtils class.
 */
public class MdcUtilsUnitTest {

    private static final int RID_LEN = 8;

    @BeforeEach
    public void before() {
        MdcUtils.clear();
    }

    @Test
    public void testGenerateRid() {
        assertNotNull(MdcUtils.generateRid());
        assertEquals(RID_LEN, MdcUtils.generateRid().length());
        assertTrue(StringUtils.isAsciiPrintable(MdcUtils.generateRid()));
    }

    @Test
    @Disabled("Run manually due to fail probability and long run time")
    public void testGenerateRidUniqness() {
        int cardinality = 10000000;
        Set<String> rids = new HashSet<>(cardinality);
        for (int i = 0; i < cardinality; i++) {
            rids.add(MdcUtils.generateRid());
        }

        assertEquals(cardinality, rids.size());
    }

    @Test
    @SneakyThrows
    public void testGetRidTimeNs() {
        assertEquals(0L, MdcUtils.getRidTimeNs());
        MdcUtils.putRid();
        Thread.sleep(1L);
        assertTrue(MdcUtils.getExecTimeMs() > 0);
    }

    @Test
    @SneakyThrows
    public void shouldReturnExecTime0ForMissingRid() {
        assertEquals(0L, MdcUtils.getRidTimeNs());
        MdcUtils.put("key", "value");
        Thread.sleep(1L);
        assertEquals(0, MdcUtils.getExecTimeMs());
    }

    @Test
    public void testPutRid() {
        assertNull(MdcUtils.getRid());
        MdcUtils.put("key", "value");
        assertEquals("value", MDC.get("key"));

        assertNull(MdcUtils.getRid());

        MdcUtils.putRid("myRid");
        assertEquals("myRid", MdcUtils.getRid());
        assertEquals("myRid", MDC.get("rid"));

        MdcUtils.clear();

        assertNull(MdcUtils.getRid());
        assertNull(MDC.get("key"));
    }

}
