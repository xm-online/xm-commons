package com.icthh.xm.commons.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.logging.util.MdcUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Set;

/**
 * Test for MdcUtils class.
 */
public class MdcUtilsUnitTest {

    private static final int RID_LEN = 8;

    @Before
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
    @Ignore("Run manually due to fail probability and long run time")
    public void testGenerateRidUniqness() {
        int cardinality = 10000000;
        Set<String> rids = new HashSet<>(cardinality);
        for (int i = 0; i < cardinality; i++) {
            rids.add(MdcUtils.generateRid());
        }

        assertEquals(cardinality, rids.size());
    }

    @Test
    public void testGetRidTimeNs() {
        assertEquals(0L, MdcUtils.getRidTimeNs() );
        MdcUtils.put("key", "value");
        assertTrue(MdcUtils.getExecTimeMs() > 0);
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
