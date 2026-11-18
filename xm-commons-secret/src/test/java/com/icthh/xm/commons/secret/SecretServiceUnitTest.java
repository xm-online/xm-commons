package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.secret.SecretTestSupport.provider;
import static com.icthh.xm.commons.secret.SecretTestSupport.tenantHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretServiceUnitTest {

    @Mock
    private SecretStore store;

    private SecretProperties properties;
    private SecretService service;

    @BeforeEach
    void setUp() {
        properties = new SecretProperties();
        service = new SecretService(tenantHolder("xm"), properties, provider(store));
    }

    @Test
    void putReturnsReferenceAndWritesTenantPath() {
        assertThat(service.put("crm/token", Map.of("value", "v"))).isEqualTo("secret:crm/token");
        verify(store).write("XM/crm/token", Map.of("value", "v"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "a/b", "a.b-c_d/e1", "...", "a..b"})
    void acceptsValidAlias(String alias) {
        service.put(alias, Map.of("k", "v"));
        verify(store).write("XM/" + alias, Map.of("k", "v"));
    }

    @Test
    void accepts256CharAlias() {
        service.put("a".repeat(256), Map.of("k", "v"));
        verify(store).write(any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "/a", "a/", "a//b", ".", "..", "a/../b", "a/./b", "a b", "a#b", "a\\b", "%2e%2e",
        "../a", "a/..", "ä"})
    void rejectsInvalidAliasBeforeStore(String alias) {
        assertThatThrownBy(() -> service.get(alias)).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", SecretService.ERROR_ALIAS_INVALID);
        assertThatThrownBy(() -> service.put(alias, Map.of("k", "v"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(alias)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(store);
    }

    @Test
    void rejects257CharAlias() {
        assertThatThrownBy(() -> service.get("a".repeat(257))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(store);
    }

    @Test
    void failsWithoutTenant() {
        SecretService noTenant = new SecretService(tenantHolder(null), properties, provider(store));
        assertThatThrownBy(() -> noTenant.get("a")).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("tenant");
        verifyNoInteractions(store);
    }

    @Test
    void disabledFailsClearlyButResolvePassesPlainValues() {
        SecretService disabled = new SecretService(tenantHolder("xm"), properties, provider(null));
        assertThatThrownBy(() -> disabled.get("a")).hasMessageContaining("Secrets are disabled");
        assertThatThrownBy(() -> disabled.put("a", Map.of("k", "v"))).hasMessageContaining("Secrets are disabled");
        assertThatThrownBy(() -> disabled.delete("a")).hasMessageContaining("Secrets are disabled");
        assertThatThrownBy(() -> disabled.get("a", "f")).hasMessageContaining("Secrets are disabled");
        assertThat(disabled.resolve("plain")).isEqualTo("plain");
    }

    @Test
    void missingSecretAndFieldAreNotFound() {
        when(store.read("XM/a")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("a")).isInstanceOf(SecretNotFoundException.class)
            .hasFieldOrPropertyWithValue("code", SecretNotFoundException.CODE)
            .hasMessage("Secret not found: a");

        when(store.read("XM/b")).thenReturn(Optional.of(Map.of("k", "v")));
        assertThatThrownBy(() -> service.get("b", "nope")).isInstanceOf(SecretNotFoundException.class);
        assertThat(service.get("b", "k")).isEqualTo("v");
    }

    @Test
    void resolveParsesReferences() {
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("f", "one")));
        when(store.read("XM/two")).thenReturn(Optional.of(Map.of("f", "1", "g", "2")));

        assertThat(service.resolve(null)).isNull();
        assertThat(service.resolve("plain")).isEqualTo("plain");
        assertThat(service.resolve("secret:a#f")).isEqualTo("one");
        assertThat(service.resolve("secret:a")).isEqualTo("one");
        assertThat(service.resolve("secret:two#g")).isEqualTo("2");
        assertThatThrownBy(() -> service.resolve("secret:two")).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", SecretService.ERROR_FIELD_REQUIRED);
        assertThatThrownBy(() -> service.resolve("secret:bad alias#f")).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", SecretService.ERROR_ALIAS_INVALID);
        assertThatThrownBy(() -> service.resolve("secret:a#")).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", SecretService.ERROR_REFERENCE_INVALID);
    }

    @Test
    void putRejectsEmptyOrNullData() {
        assertThatThrownBy(() -> service.put("a", Map.of())).isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", SecretService.ERROR_DATA_INVALID);
        assertThatThrownBy(() -> service.put("a", null)).isInstanceOf(BusinessException.class);
        verify(store, never()).write(any(), any());
    }

    @Test
    void secondReadComesFromCache() {
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "v")));
        service.get("a");
        service.get("a");
        verify(store, times(1)).read("XM/a");
    }

    @Test
    void putAndDeleteEvictTheEntry() {
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "old")), Optional.of(Map.of("k", "new")),
            Optional.empty());
        assertThat(service.get("a")).containsEntry("k", "old");
        service.put("a", Map.of("k", "new"));
        assertThat(service.get("a")).containsEntry("k", "new");
        service.delete("a");
        verify(store).delete("XM/a");
        assertThatThrownBy(() -> service.get("a")).isInstanceOf(SecretNotFoundException.class);
    }

    @Test
    void missingSecretIsNotCached() {
        when(store.read("XM/a")).thenReturn(Optional.empty(), Optional.of(Map.of("k", "v")));
        assertThatThrownBy(() -> service.get("a")).isInstanceOf(SecretNotFoundException.class);
        assertThat(service.get("a")).containsEntry("k", "v");
    }

    @Test
    void zeroTtlDisablesCache() {
        properties.setCacheTtl(Duration.ZERO);
        SecretService noCache = new SecretService(tenantHolder("xm"), properties, provider(store));
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "v")));
        noCache.get("a");
        noCache.get("a");
        verify(store, times(2)).read("XM/a");
    }

    @Test
    void returnedMapIsUnmodifiable() {
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "v")));
        Map<String, String> data = service.get("a");
        assertThatThrownBy(() -> data.put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void tenantsDoNotShareCache() {
        TenantContextHolder holder = tenantHolder("xm");
        SecretService shared = new SecretService(holder, properties, provider(store));
        when(store.read("XM/a")).thenReturn(Optional.of(Map.of("k", "xm")));
        when(store.read("OTHER/a")).thenReturn(Optional.of(Map.of("k", "other")));
        assertThat(shared.get("a")).containsEntry("k", "xm");
        SecretTestSupport.switchTenant(holder, "other");
        assertThat(shared.get("a")).containsEntry("k", "other");
    }
}
