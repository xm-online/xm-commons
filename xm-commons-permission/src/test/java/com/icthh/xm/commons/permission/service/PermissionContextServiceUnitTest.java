package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.permission.domain.dto.PermissionContextDto;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
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
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionContextServiceUnitTest {

    private static final String TOKEN = UUID.randomUUID().toString();

    private RestTemplate restTemplate;
    private PermissionContextService permissionContextService;

    private static final String APPLICATION_NAME = "service";
    private static final String PERMISSION_CONTEXT_URI = "uaa/api/account";

    @Before
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        permissionContextService = new PermissionContextService(restTemplate);

        setField(permissionContextService, "applicationName", APPLICATION_NAME);
        setField(permissionContextService, "permissionContextUri", PERMISSION_CONTEXT_URI);

        // Mock SecurityContextHolder
        SecurityContext securityContext = mock(SecurityContext.class);
        XmAuthentication auth = mock(XmAuthentication.class);
        XmAuthenticationDetails authDetails = mock(XmAuthenticationDetails.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getDetails()).thenReturn(authDetails);
        when(authDetails.getTokenValue()).thenReturn(TOKEN);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void hasPermission_returnsTrue_whenPermissionAndContextMatch() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of("role", "admin"));

        boolean result = permissionContextService.hasPermission("READ_PRIVILEGE", Map.of("role", "admin"));

        assertThat(result).isTrue();
    }

    @Test
    public void hasPermission_returnsFalse_whenPermissionMissing() {
        mockPermissionContextDto(List.of("WRITE_PRIVILEGE"), Map.of("role", "admin"));

        boolean result = permissionContextService.hasPermission("READ_PRIVILEGE", Map.of("role", "admin"));

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenContextKeyMismatch() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of("role", "user"));

        boolean result = permissionContextService.hasPermission("READ_PRIVILEGE", Map.of("role", "admin"));

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenContextKeyMissing() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of("role", "user"));

        boolean result = permissionContextService.hasPermission("READ_PRIVILEGE", Map.of("lucky", "user"));

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermission_returnsFalse_whenContextKeyValueNull() {
        mockPermissionContextDto(List.of("READ_PRIVILEGE"), Map.of());

        Map<String, Object> input = new HashMap<>();
        input.put("lucky", null);
        boolean result = permissionContextService.hasPermission("READ_PRIVILEGE", input);

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
