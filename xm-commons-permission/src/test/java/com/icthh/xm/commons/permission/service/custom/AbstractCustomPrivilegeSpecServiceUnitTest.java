package com.icthh.xm.commons.permission.service.custom;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractCustomPrivilegeSpecServiceUnitTest {

    private static final String TENANT_KEY = "TEST";
    private static final String PRIVILEGES_PATH = "/config/tenants/TEST/custom-privileges.yml";

    private static final String EXISTING_PRIVILEGES_YAML = """
        ---
        applications:
        - key: "APPLICATION.ACCOUNT.USER"
        - key: "APPLICATION.PRODUCT"
        """;

    @Mock
    private CommonConfigRepository commonConfigRepository;

    @Mock
    private CustomPrivilegesExtractor applicationExtractor;

    @Captor
    private ArgumentCaptor<Configuration> configurationCaptor;

    private TestCustomPrivilegeSpecService service;

    @Before
    public void before() {
        List<CustomPrivilegesExtractor> extractors = List.of(applicationExtractor);
        service = new TestCustomPrivilegeSpecService(commonConfigRepository, extractors);
    }

    @Test
    public void shouldResolvePathAndUpdateCustomPrivileges() {
        ArrayList<Map<String, String>> privileges = new ArrayList<>(List.of(
            Map.of("key", "APPLICATION.ACCOUNT.USER")
        ));

        when(commonConfigRepository.getConfig(isNull(), eq(List.of(PRIVILEGES_PATH)))).thenReturn(new HashMap<>());

        when(applicationExtractor.isEnabled(TENANT_KEY)).thenReturn(true);
        when(applicationExtractor.getSectionName()).thenReturn("applications");
        Collection<ConfigWithKey> specs = new ArrayList<>();
        when(applicationExtractor.toPrivileges(eq(specs))).thenReturn(privileges);

        service.onSpecificationUpdate(specs, TENANT_KEY);

        verify(commonConfigRepository).getConfig(isNull(), eq(List.of(PRIVILEGES_PATH)));
        verify(commonConfigRepository).updateConfigFullPath(configurationCaptor.capture(), isNull());
        assertEquals(PRIVILEGES_PATH, configurationCaptor.getValue().getPath());
    }

    @Test
    public void shouldReturnConfigurationWhenPathExists() {
        Configuration expected = new Configuration(PRIVILEGES_PATH, EXISTING_PRIVILEGES_YAML);
        Map<String, Configuration> configs = Map.of(PRIVILEGES_PATH, expected);
        when(commonConfigRepository.getConfig(isNull(), eq(List.of(PRIVILEGES_PATH)))).thenReturn(new HashMap<>(configs));

        Configuration result = service.getConfigByPath(PRIVILEGES_PATH);

        assertNotNull(result);
        assertEquals(EXISTING_PRIVILEGES_YAML, result.getContent());
    }

    @Test
    public void shouldReturnNullWhenPathNotFound() {
        when(commonConfigRepository.getConfig(isNull(), eq(List.of(PRIVILEGES_PATH)))).thenReturn(new HashMap<>());

        Configuration result = service.getConfigByPath(PRIVILEGES_PATH);

        assertNull(result);
    }

    @Test
    public void shouldUpdateWhenContentChanged() {
        Configuration existingConfig = new Configuration(PRIVILEGES_PATH, EXISTING_PRIVILEGES_YAML);
        String expectedHash = DigestUtils.sha1Hex(EXISTING_PRIVILEGES_YAML);
        ArrayList<Map<String, String>> privileges = new ArrayList<>(List.of(
            Map.of("key", "APPLICATION.ACCOUNT.USER"),
            Map.of("key", "APPLICATION.PRODUCT"),
            Map.of("key", "APPLICATION.NEW_PRIVILEGE")
        ));

        when(applicationExtractor.isEnabled(TENANT_KEY)).thenReturn(true);
        when(applicationExtractor.getSectionName()).thenReturn("applications");
        Collection<ConfigWithKey> specs = new ArrayList<>();
        when(applicationExtractor.toPrivileges(eq(specs))).thenReturn(privileges);

        service.updateCustomPrivileges(specs, PRIVILEGES_PATH, existingConfig, TENANT_KEY);

        verify(commonConfigRepository).updateConfigFullPath(configurationCaptor.capture(), eq(expectedHash));
        String content = configurationCaptor.getValue().getContent();
        assertTrue(content.contains("APPLICATION.NEW_PRIVILEGE"));
    }

    @Test
    public void shouldNotUpdateWhenContentNotChangedInCustomOrder() {
        String customOrderedPrivileges = """
            ---
            applications:
            - key: "APPLICATION.PRODUCT"
            - key: "APPLICATION.RATE"
            - key: "APPLICATION.ACCOUNT.USER"
            """;
        Configuration existingConfig = new Configuration(PRIVILEGES_PATH, customOrderedPrivileges);
        ArrayList<Map<String, String>> privileges = new ArrayList<>(List.of(
            Map.of("key", "APPLICATION.ACCOUNT.USER"),
            Map.of("key", "APPLICATION.PRODUCT"),
            Map.of("key", "APPLICATION.RATE")
        ));

        when(applicationExtractor.isEnabled(TENANT_KEY)).thenReturn(true);
        when(applicationExtractor.getSectionName()).thenReturn("applications");
        Collection<ConfigWithKey> specs = new ArrayList<>();
        when(applicationExtractor.toPrivileges(eq(specs))).thenReturn(privileges);

        service.updateCustomPrivileges(specs, PRIVILEGES_PATH, existingConfig, TENANT_KEY);

        verify(commonConfigRepository, never()).updateConfigFullPath(configurationCaptor.capture(), eq(DigestUtils.sha1Hex(customOrderedPrivileges)));
    }

    @Test
    public void shouldPassNullHashWhenExistingConfigIsNull() {
        ArrayList<Map<String, String>> privileges = new ArrayList<>(List.of(
            Map.of("key", "APPLICATION.ACCOUNT.USER")
        ));

        when(applicationExtractor.isEnabled(TENANT_KEY)).thenReturn(true);
        when(applicationExtractor.getSectionName()).thenReturn("applications");
        Collection<ConfigWithKey> specs = new ArrayList<>();
        when(applicationExtractor.toPrivileges(eq(specs))).thenReturn(privileges);

        service.updateCustomPrivileges(specs, PRIVILEGES_PATH, null, TENANT_KEY);

        verify(commonConfigRepository).updateConfigFullPath(configurationCaptor.capture(), isNull());
    }

    @Test
    public void shouldUpdateWithCorrectPath() {
        ArrayList<Map<String, String>> privileges = new ArrayList<>(List.of(
            Map.of("key", "APPLICATION.PRODUCT")
        ));

        when(applicationExtractor.isEnabled(TENANT_KEY)).thenReturn(true);
        when(applicationExtractor.getSectionName()).thenReturn("applications");
        Collection<ConfigWithKey> specs = new ArrayList<>();
        when(applicationExtractor.toPrivileges(eq(specs))).thenReturn(privileges);

        service.updateCustomPrivileges(specs, PRIVILEGES_PATH, null, TENANT_KEY);

        verify(commonConfigRepository).updateConfigFullPath(configurationCaptor.capture(), isNull());
        assertEquals(PRIVILEGES_PATH, configurationCaptor.getValue().getPath());
    }

    @Test
    public void shouldHandleExistingConfigWithEmptyContent() {
        String blankContent = "   ";
        Configuration blankConfig = new Configuration(PRIVILEGES_PATH, blankContent);
        String expectedHash = DigestUtils.sha1Hex(blankContent);
        ArrayList<Map<String, String>> privileges = new ArrayList<>(List.of(
            Map.of("key", "APPLICATION.PRODUCT")
        ));

        when(applicationExtractor.isEnabled(TENANT_KEY)).thenReturn(true);
        when(applicationExtractor.getSectionName()).thenReturn("applications");
        Collection<ConfigWithKey> specs = new ArrayList<>();
        when(applicationExtractor.toPrivileges(eq(specs))).thenReturn(privileges);

        service.updateCustomPrivileges(specs, PRIVILEGES_PATH, blankConfig, TENANT_KEY);

        verify(commonConfigRepository).updateConfigFullPath(configurationCaptor.capture(), eq(expectedHash));
    }
}
