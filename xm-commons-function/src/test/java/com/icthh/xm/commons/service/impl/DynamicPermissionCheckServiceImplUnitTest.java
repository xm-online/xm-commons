package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionApiSpec;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicPermissionCheckServiceImplUnitTest {

    private static final String TENANT = "TEST_TENANT";

    @Mock
    private PermissionCheckService permissionCheckService;

    @Mock
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private FunctionApiSpecConfiguration functionApiSpecConfiguration;

    @InjectMocks
    private DynamicPermissionCheckServiceImpl dynamicPermissionCheckService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void isDynamicFunctionPermissionEnabled() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TENANT);

        FunctionApiSpec mockFunctionApiSpec = mock(FunctionApiSpec.class);
        when(mockFunctionApiSpec.isDynamicPermissionCheckEnabled()).thenReturn(true);

        when(functionApiSpecConfiguration.getSpecByTenant(TENANT)).thenReturn(Optional.of(mockFunctionApiSpec));

        Boolean result = dynamicPermissionCheckService.isDynamicFunctionPermissionEnabled();
        assertTrue(result);
    }
}
