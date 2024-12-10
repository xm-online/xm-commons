package com.icthh.xm.commons.service.impl;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static com.icthh.xm.commons.utils.Constants.FUNCTIONS;
import static com.icthh.xm.commons.utils.Constants.TENANT_CONFIG_DYNAMIC_CHECK_ENABLED;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private TenantConfigService tenantConfigService;

    @InjectMocks
    private DynamicPermissionCheckServiceImpl dynamicPermissionCheckService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void isDynamicFunctionPermissionEnabled() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TENANT);

        Map<String, Object> tenantConfig = Map.of(FUNCTIONS, Map.of(TENANT_CONFIG_DYNAMIC_CHECK_ENABLED, true));
        when(tenantConfigService.getConfig()).thenReturn(tenantConfig);

        Boolean result = dynamicPermissionCheckService.isDynamicFunctionPermissionEnabled();
        assertTrue(result);
    }
}
