package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.TenantScriptStorage.CLASSPATH;
import static com.icthh.xm.commons.lep.TenantScriptStorage.FILE;
import static com.icthh.xm.commons.lep.TenantScriptStorage.XM_MS_CONFIG;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TenantScriptStorageUnitTest {

    private static final String TENANT_KEY = "xm";
    private static final String APP_NAME = "activation";
    private static final String PATH = "/commons/functions/aggregation";
    private static final String URL_PREFIX_COMMONS_ENVIRONMENT = "/commons/environment";
    private static final String URL_PREFIX_COMMONS_TENANT = "/commons/tenant";

    @Test
    public void testResolveLepForClassPathScriptStorage() {
        String envLepPath = CLASSPATH.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_ENVIRONMENT + PATH);
        String tenantLepPath = CLASSPATH.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_TENANT + PATH);
        String localLepPath = CLASSPATH.resolvePath(TENANT_KEY, APP_NAME, PATH);

        assertEquals("classpath:/lep/custom/commons/functions/aggregation", envLepPath);
        assertEquals("classpath:/lep/custom/xm/commons/functions/aggregation", tenantLepPath);
        assertEquals("classpath:/lep/custom/xm/commons/functions/aggregation", localLepPath);
    }

    @Test
    public void testResolveLepForXmConfigScriptStorage() {
        String envLepPath = XM_MS_CONFIG.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_ENVIRONMENT + PATH);
        String tenantLepPath = XM_MS_CONFIG.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_TENANT + PATH);
        String localLepPath = XM_MS_CONFIG.resolvePath(TENANT_KEY, APP_NAME, PATH);

        assertEquals("xm-ms-config:/config/tenants/commons/lep/commons/functions/aggregation", envLepPath);
        assertEquals("xm-ms-config:/config/tenants/XM/commons/lep/commons/functions/aggregation", tenantLepPath);
        assertEquals("xm-ms-config:/config/tenants/XM/activation/lep/commons/functions/aggregation", localLepPath);
    }

    @Test
    public void testResolveLepForFileScriptStorage() {
        String envLepPath = FILE.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_ENVIRONMENT + PATH);
        String tenantLepPath = FILE.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_TENANT + PATH);
        String localLepPath = FILE.resolvePath(TENANT_KEY, APP_NAME, PATH);

        String basePath = "file://" + FileSystemUtils.APP_HOME_DIR;

        assertEquals(separatorsToSystem("/config/tenants/commons/lep/commons/functions/aggregation"),
            envLepPath.substring(basePath.length()));
        assertEquals(separatorsToSystem("/config/tenants/XM/commons/lep/commons/functions/aggregation"),
            tenantLepPath.substring(basePath.length()));
        assertEquals(separatorsToSystem("/config/tenants/XM/activation/lep/commons/functions/aggregation"),
            localLepPath.substring(basePath.length()));
    }
}
