package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.permission.domain.dto.PermissionContextDto;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionContextCheckServiceUnitTest {

    private static final String TOKEN = UUID.randomUUID().toString();

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    private PermissionContextCheckService permissionContextCheckService;

    private static final String TENANT = "TENANT_KEY";
    private static final String APPLICATION_NAME = "service";
    private static final String PERMISSION_CONTEXT_URI = "uaa/api/account";
    private static final String CUSTOM_PRIVILEGES_PATH = "/config/tenants/{tenantName}/custom-privileges.yml";
    private static final String CUSTOM_PRIVILEGES_CONTENT = ""
        + "context:\n"
        + "- key: \"READ_PRIVILEGE\"\n"
        + "- key: \"WRITE_PRIVILEGE\"\n"
        + "test:\n"
        + "- key: \"TEST_PRIVILEGE\"\n";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        permissionContextCheckService = new PermissionContextCheckService(
            restTemplate,
            tenantConfigRepository,
            tenantContextHolder
        );

        setField(permissionContextCheckService, "applicationName", APPLICATION_NAME);
        setField(permissionContextCheckService, "permissionContextUri", PERMISSION_CONTEXT_URI);
        setField(permissionContextCheckService, "customPrivilegesPath", CUSTOM_PRIVILEGES_PATH);

        // Mock SecurityContextHolder
        SecurityContext securityContext = mock(SecurityContext.class);
        XmAuthentication auth = mock(XmAuthentication.class);
        XmAuthenticationDetails authDetails = mock(XmAuthenticationDetails.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getDetails()).thenReturn(authDetails);
        when(authDetails.getTokenValue()).thenReturn(TOKEN);

        SecurityContextHolder.setContext(securityContext);

        // Mock TenantContextHolder
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));
    }

    @Test
    public void hasPermission_returnsTrue_whenPermissionAndContextMatch() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of("role", "admin"));

        boolean result = permissionContextCheckService.hasPermission("READ_PRIVILEGE", Map.of("role", "admin"));

        assertThat(result).isTrue();
    }

    @Test
    public void hasPermission_returnsFalse_whenPermissionMissing() {
        mockPermissionContextDto(List.of("WRITE_PRIVILEGE"), Map.of("role", "admin"));

        boolean result = permissionContextCheckService.hasPermission("READ_PRIVILEGE", Map.of("role", "admin"));

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenContextKeyMismatch() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of("role", "user"));

        boolean result = permissionContextCheckService.hasPermission("READ_PRIVILEGE", Map.of("role", "admin"));

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenContextKeyMissing() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of("role", "user"));

        boolean result = permissionContextCheckService.hasPermission("READ_PRIVILEGE", Map.of("lucky", "user"));

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenContextKeyValueNull() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of());

        Map<String, Object> input = new HashMap<>();
        input.put("lucky", null);
        boolean result = permissionContextCheckService.hasPermission("READ_PRIVILEGE", input);

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenPermissionNotExist() {
        mockPermissionContextDto(List.of("NOT_EXISTING_PERMISSION"), Map.of("role", "admin"));

        boolean result = permissionContextCheckService.hasPermission("NOT_EXISTING_PERMISSION", Map.of("role", "admin"));

        assertThat(result).isFalse();
    }

    @SneakyThrows
    private void mockPermissionContextDto(List<String> permissions, Map<String, Object> ctx) {
        PermissionContextDto dto = new PermissionContextDto();
        dto.setPermissions(permissions);
        dto.setCtx(ctx);

        Map<String, PermissionContextDto> serviceContextMapping = new TreeMap<>();
        serviceContextMapping.put(APPLICATION_NAME, dto);

        Map<String, Object> contextWrapper = Map.of("context", serviceContextMapping);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(contextWrapper, HttpStatus.OK);

        when(tenantConfigRepository.getConfigFullPath(
            eq(TENANT),
            eq("/api" + CUSTOM_PRIVILEGES_PATH.replace("{tenantName}", TENANT)))
        ).thenReturn(CUSTOM_PRIVILEGES_CONTENT);

        when(restTemplate.exchange(
            eq(new URI("http://uaa/api/account")),
            eq(HttpMethod.GET),
            argThat(new TokenHeaderExists()),
            eq(Map.class)
        )).thenReturn(responseEntity);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class TokenHeaderExists implements ArgumentMatcher<HttpEntity> {

        @Override
        public boolean matches(HttpEntity actual) {
            assertEquals("Bearer " + TOKEN, actual.getHeaders().get("Authorization").iterator().next());
            return true;
        }
    }
}
