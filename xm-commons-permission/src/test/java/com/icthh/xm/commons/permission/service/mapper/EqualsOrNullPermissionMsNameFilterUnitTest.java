package com.icthh.xm.commons.permission.service.mapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.permission.service.filter.EqualsOrNullPermissionMsNameFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class EqualsOrNullPermissionMsNameFilterUnitTest {

    private EqualsOrNullPermissionMsNameFilter filter;
    private final static String APP_NAME = "appName";

    @Before
    public void initPrivilegeScanner() {
        this.filter = new EqualsOrNullPermissionMsNameFilter();
    }

    @Test
    public void testFilterPermission() {
        assertTrue(filter.filterPermission(null));
        assertTrue(filter.filterPermission(""));
        assertTrue(filter.filterPermission("other app name"));
        assertTrue(filter.filterPermission(APP_NAME));

        ReflectionTestUtils.setField(filter, "msName", APP_NAME, String.class);

        assertFalse(filter.filterPermission(null));
        assertFalse(filter.filterPermission(""));
        assertFalse(filter.filterPermission("other app name"));
        assertTrue(filter.filterPermission(APP_NAME));
    }
}
