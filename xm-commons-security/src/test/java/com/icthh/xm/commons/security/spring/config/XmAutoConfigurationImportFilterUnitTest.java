package com.icthh.xm.commons.security.spring.config;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

public class XmAutoConfigurationImportFilterUnitTest {

    private static final String USER_DETAILS =
        "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration";
    private static final String OTHER = "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration";

    @Test
    public void testUserDetailsServiceAutoConfigurationIsSkipped() {
        XmAutoConfigurationImportFilter filter = createFilter(new MockEnvironment());

        assertArrayEquals(new boolean[] {true, false}, filter.match(new String[] {OTHER, USER_DETAILS}, null));
    }

    @Test
    public void testCandidatesSkippedByEarlierFiltersAreIgnored() {
        XmAutoConfigurationImportFilter filter = createFilter(new MockEnvironment());

        assertArrayEquals(new boolean[] {true, true, false},
            filter.match(new String[] {OTHER, null, USER_DETAILS}, null));
    }

    @Test
    public void testFilterCanBeDisabled() {
        XmAutoConfigurationImportFilter filter = createFilter(
            new MockEnvironment().withProperty(XmAutoConfigurationImportFilter.DISABLED_PROPERTY, "true"));

        assertArrayEquals(new boolean[] {true, true}, filter.match(new String[] {OTHER, USER_DETAILS}, null));
    }

    private XmAutoConfigurationImportFilter createFilter(MockEnvironment environment) {
        XmAutoConfigurationImportFilter filter = new XmAutoConfigurationImportFilter();
        filter.setEnvironment(environment);
        return filter;
    }
}
