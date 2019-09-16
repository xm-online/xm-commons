package com.icthh.xm.commons.config.client.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.config.domain.TenantState;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class TenantListRepositoryUnitTest {

    private TenantListRepository tenantListRepository;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CommonConfigRepository commonConfigRepository;

    private String applicationName = "entity";
    @Spy
    private XmConfigProperties xmConfigProperties;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    @SneakyThrows
    public void init() {

        MockitoAnnotations.initMocks(this);

        when(xmConfigProperties.getXmConfigUrl()).thenReturn("xm-config-url");

        Configuration configuration = new Configuration();
        configuration.setPath("<mock>");
        configuration.setContent(readFile("/config/tenants/tenants-list.json"));

        when(commonConfigRepository.getConfig(any(), anyCollection())).thenReturn(Collections.singletonMap(
            TenantListRepository.TENANTS_LIST_CONFIG_KEY, configuration));

        tenantListRepository = new TenantListRepository(restTemplate,
                                                        commonConfigRepository,
                                                        applicationName,
                                                        xmConfigProperties);

    }

    @Test
    public void shouldFailTenantListNotFound() {

        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tenant list not found. Maybe xm-config not running.");

        when(commonConfigRepository.getConfig(any(), anyCollection())).thenReturn(Collections.singletonMap(
            TenantListRepository.TENANTS_LIST_CONFIG_KEY, null));

        new TenantListRepository(restTemplate, commonConfigRepository, applicationName, xmConfigProperties);
    }

    @Test
    public void shouldFailTenantListForAppNameisEmpty() {

        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tenant list for wrong-app empty. Check tenants-list.json.");

        new TenantListRepository(restTemplate, commonConfigRepository, "wrong-app", xmConfigProperties);
    }

    @Test
    public void testGetTenants() {

        Set<String> tenants = tenantListRepository.getTenants();

        assertNotNull(tenants);
        assertEquals(3, tenants.size());
        assertThat(tenants).containsExactlyInAnyOrder("xm", "demo", "susp");

        Set<String> suspendedTenants = tenantListRepository.getSuspendedTenants();
        assertNotNull(suspendedTenants);
        assertEquals(1, suspendedTenants.size());
        assertThat(suspendedTenants).containsExactlyInAnyOrder("susp");
    }

    @Test
    public void testRefreshTenants() {

        Set<String> tenants = tenantListRepository.getTenants();

        assertNotNull(tenants);
        assertEquals(3, tenants.size());
        assertThat(tenants).containsExactlyInAnyOrder("xm", "demo", "susp");

        tenantListRepository.onRefresh("/config/tenants/tenants-list.json",
                                       readFile("/config/tenants/tenants-list-updated.json"));

        tenants = tenantListRepository.getTenants();

        assertNotNull(tenants);
        assertEquals(4, tenants.size());
        assertThat(tenants).containsExactlyInAnyOrder("xm", "demo", "susp", "added");

    }

    @Test
    public void testGetTenantsIncluded() {

        Set<String> tenants = tenantListRepository.getTenants();

        assertNotNull(tenants);
        assertEquals(3, tenants.size());

        Set<String> included = new HashSet<>();
        included.add("xm");
        included.add("DEMO");

        when(xmConfigProperties.getIncludeTenants()).thenReturn(included);

        tenantListRepository = new TenantListRepository(restTemplate,
                                                        commonConfigRepository,
                                                        applicationName,
                                                        xmConfigProperties);

        tenants = tenantListRepository.getTenants();

        assertNotNull(tenants);
        assertEquals(2, tenants.size());
        assertThat(tenants).containsExactlyInAnyOrder("xm","demo");

    }

    @Test
    public void ensureTenantStateConstructorExists(){
        assertNotNull(TenantState.of().name("name").state("state").build());
        new TenantState();
        new TenantState("name", "state");
    }

    @SneakyThrows
    private String readFile(String path) {
        return new String(Files.readAllBytes(Paths.get(TenantListRepository.class.getResource(path).toURI())));
    }

}
