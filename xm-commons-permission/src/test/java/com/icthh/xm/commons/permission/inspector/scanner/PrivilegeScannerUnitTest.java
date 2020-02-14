package com.icthh.xm.commons.permission.inspector.scanner;

import com.icthh.xm.commons.permission.domain.Privilege;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.reflections.Reflections;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrivilegeScannerUnitTest {

    @Mock
    private Reflections reflections;

    private PrivilegeScanner privilegeScanner;

    @Before
    public void initPrivilegeScanner() {
        this.reflections = mock(Reflections.class);
        this.privilegeScanner = new PrivilegeScanner(reflections);
        ReflectionTestUtils.setField(privilegeScanner, "appName", "TEST-APP-NAME");
    }

    @Test
    public void testScanPrivileges() {
        Method[] classMethods = TestClassWithPrivileges.class.getDeclaredMethods();
        Set<Method> methodSet = new HashSet<>();
        Collections.addAll(methodSet, classMethods);

        when(reflections.getMethodsAnnotatedWith((Class<? extends Annotation>) any())).thenReturn(methodSet);

        Set<Privilege> privilegeSet = privilegeScanner.scan();

        assertNotNull(privilegeSet);
        assertEquals(10, privilegeSet.size());

        assertPrivilegesWithDescriptions(privilegeSet);

        assertPrivilegesWithoutDescriptions(privilegeSet);

    }

    private void assertPrivilegesWithDescriptions(Set<Privilege> privilegeSet) {
        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "TEST.FIND.WITH.PERMISSION".equals(permission.getKey())));
        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "test description for find with permission method".equals(permission.getCustomDescription())));

        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "TEST.PRE.AUTHORIZE".equals(permission.getKey())));
        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "test description for pre authorize method".equals(permission.getCustomDescription())));

        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "TEST.POST.AUTHORIZE".equals(permission.getKey())));
        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "test description for post authorize method".equals(permission.getCustomDescription())));

        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "TEST.POST.FILTER".equals(permission.getKey())));
        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "test description for post filter method".equals(permission.getCustomDescription())));

        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "TEST.MANY.PRIVILEGES".equals(permission.getKey())));
        assertTrue(privilegeSet.stream().anyMatch(permission ->
            "test description for method with many privileges annotation".equals(permission.getCustomDescription())));
    }

    private void assertPrivilegesWithoutDescriptions(Set<Privilege> privilegeSet) {
        Set<Privilege> privilegesWithoutDescriptions = privilegeSet
                                                       .stream()
                                                       .filter(permission -> isNull(permission.getCustomDescription()))
                                                       .collect(toSet());

        assertEquals(5, privilegesWithoutDescriptions.size());

        assertTrue(privilegesWithoutDescriptions.stream().anyMatch(permission ->
            "TEST.FIND.WITH.PERMISSION.WITHOUT.DESCRIPTION".equals(permission.getKey())));
        assertTrue(privilegesWithoutDescriptions.stream().anyMatch(permission ->
            "TEST.PRE.AUTHORIZE.WITHOUT.DESCRIPTION".equals(permission.getKey())));
        assertTrue(privilegesWithoutDescriptions.stream().anyMatch(permission ->
            "TEST.POST.AUTHORIZE.WITHOUT.DESCRIPTION".equals(permission.getKey())));
        assertTrue(privilegesWithoutDescriptions.stream().anyMatch(permission ->
            "TEST.POST.FILTER.WITHOUT.DESCRIPTION".equals(permission.getKey())));
        assertTrue(privilegesWithoutDescriptions.stream().anyMatch(permission ->
            "TEST.MANY.PRIVILEGES.WITHOUT.DESCRIPTION".equals(permission.getKey())));
    }
}
