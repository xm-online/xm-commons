package com.icthh.xm.commons.config.client.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class TenantConfigServiceTest {

    public static final String CONFIG_PATH_DEFAULT = "/config/tenants/TEST/tenant-config.yml";

    public static final String CONFIG_PATH_CUSTOM = "/config/tenants/TEST/tenant-config2.yml";

    private TenantConfigService tenantConfigService;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private XmConfigProperties xmConfigProperties;

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);

        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenant()).thenReturn(Optional.of(new PlainTenant(TenantKey.valueOf("TEST"))));
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        when(xmConfigProperties.getTenantConfigPattern()).thenReturn("/config/tenants/{tenantName}/tenant-config.yml");

        tenantConfigService = new TenantConfigService(xmConfigProperties, tenantContextHolder);

        tenantConfigService.onInit(CONFIG_PATH_DEFAULT, readConfigFile(CONFIG_PATH_DEFAULT));
    }

    @Test
    public void testGetConfigValue() throws IOException {

        assertNotNull(tenantConfigService.getConfig());
        assertEquals("value1", getConfig().get("testProperty"));

    }

    @Test
    public void testGetConfigValueFromDefaultPath() throws IOException {

        when(xmConfigProperties.getTenantConfigPattern()).thenReturn("");

        assertNotNull(tenantConfigService.getConfig());
        assertEquals("value1", getConfig().get("testProperty"));

    }

    @Test
    public void testGetConfigValueFromCustomPath() throws IOException {

        when(xmConfigProperties.getTenantConfigPattern()).thenReturn("/config/tenants/{tenantName}/tenant-config2.yml");

        tenantConfigService.onInit(CONFIG_PATH_CUSTOM, readConfigFile(CONFIG_PATH_CUSTOM));

        assertNotNull(tenantConfigService.getConfig());
        assertEquals("value2", getConfig().get("testProperty"));

    }

    @Test
    public void testGetConfigValueEmpty() throws IOException {

        when(xmConfigProperties.getTenantConfigPattern()).thenReturn("/config/tenants/{tenantName}/tenant-config3.yml");
        tenantConfigService.onInit("/config/tenants/TEST/tenant-config3.yml", "");

        assertNotNull(tenantConfigService.getConfig());
        assertTrue(tenantConfigService.getConfig().isEmpty());

    }

    @Test
    public void testCanNotChangeConfigMapOutside() throws IOException {

        assertEquals("value1", getConfig().get("testProperty"));

        assertEquals(1, tenantConfigService.getConfig().size());

        try {
            tenantConfigService.getConfig().put("newProperty", "You've been hacked!");
            fail("should not be success!!!");
        } catch (UnsupportedOperationException e) {
            assertEquals(1, tenantConfigService.getConfig().size());
        }

        Map<String, Object> map = getConfig();

        try {
            map.put("testProperty", "You've been hacked!");
            fail("should not be success!!!");
        } catch (UnsupportedOperationException e) {
            assertEquals("value1", getConfig().get("testProperty"));
        }

        List<Object> list = List.class.cast(getConfig().get("testList"));

        assertEquals("item2", list.get(1));
        assertEquals(3, list.size());

        try {
            list.set(1, "replaced item!");
            fail("should not be success!!!");
        } catch (UnsupportedOperationException e) {
            assertEquals("item2", List.class.cast(getConfig().get("testList")).get(1));
        }

    }

    @Test
    public void testGetComplexConfig() throws IOException {

        when(xmConfigProperties.getTenantConfigPattern()).thenReturn
            ("/config/tenants/{tenantName}/tenant-config-complex.yml");

        String file = "/config/tenants/TEST/tenant-config-complex.yml";

        tenantConfigService.onInit(file, readConfigFile(file));

        assertNotNull(tenantConfigService.getConfig());
        assertEquals(1, tenantConfigService.getConfig().size());
        assertEquals(readConfigFile("/config/tenants/TEST/tenant-config-complex.txt"), tenantConfigService
            .getConfig().toString());

        // List of Strings
        assertEquals("item2", List.class.cast(getConfig().get("list1")).get(1));
        assertEquals(3, List.class.cast(getConfig().get("list1")).size());

        // List of Maps
        assertEquals("val2.2",
                     Map.class.cast(
                         List.class.cast(getConfig().get("list2")).get(1)).get("attr2"));
        assertEquals(2, List.class.cast(getConfig().get("list2")).size());

        // List of Map of Maps
        assertEquals("val3.2.2",
                     Map.class.cast(
                         Map.class.cast(
                             List.class.cast(getConfig().get("list3")).get(1)).get("obj1")).get("attr2"));
        assertEquals(2, List.class.cast(getConfig().get("list3")).size());

    }

    @Test
    public void testCanNotChangeInnerCollection() throws IOException {

        when(xmConfigProperties.getTenantConfigPattern()).thenReturn
            ("/config/tenants/{tenantName}/tenant-config-complex.yml");

        String file = "/config/tenants/TEST/tenant-config-complex.yml";

        tenantConfigService.onInit(file, readConfigFile(file));

        try {

            Map.class.cast(
                List.class.cast(getConfig().get("list2")).get(1)).put("attr2", "hacked!");
            fail("should not be success!!!");
        } catch (UnsupportedOperationException e) {
            assertEquals("val2.2",
                         Map.class.cast(
                             List.class.cast(getConfig().get("list2")).get(1)).get("attr2"));
        }

        try {

            Map.class.cast(
                Map.class.cast(
                    List.class.cast(getConfig().get("list3")).get(1)).get("obj1")).put("attr2", "hacked!");
            fail("should not be success!!!");
        } catch (UnsupportedOperationException e) {
            assertEquals("val3.2.2",
                         Map.class.cast(
                             Map.class.cast(
                                 List.class.cast(getConfig().get("list3")).get(1)).get("obj1")).get("attr2"));
        }

        try {

            List.class.cast(
                List.class.cast(getConfig().get("matrix")).get(1)).set(0, "hacked!");
            fail("should not be success!!!");
        } catch (UnsupportedOperationException e) {
            assertEquals("a10",
                         List.class.cast(
                             List.class.cast(getConfig().get("matrix")).get(1)).get(0));
        }

    }

    private Map<String, Object> getConfig() {
        return Map.class.cast(tenantConfigService.getConfig().get("config"));
    }

    private String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(path)))
            .lines().collect(Collectors.joining("\n"));
    }

}
