package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.lep.storage.ClassPathTenantScriptPathResolver;
import com.icthh.xm.commons.lep.storage.FileTenantScriptPathResolver;
import com.icthh.xm.commons.lep.storage.XmMsConfigTenantScriptPathResolver;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TenantScriptStorageUnitTest {

    private static final String TENANT_KEY = "xm";
    private static final String APP_NAME = "activation";
    private static final String PATH = "/commons/functions/aggregation";
    private static final String URL_PREFIX_COMMONS_ENVIRONMENT = "/commons/environment";
    private static final String URL_PREFIX_COMMONS_TENANT = "/commons/tenant";

    @Test
    public void testResolveLepForClassPathScriptStorage() {
        var classpath = new ClassPathTenantScriptPathResolver();
        String envLepPath = classpath.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_ENVIRONMENT + PATH);
        String tenantLepPath = classpath.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_TENANT + PATH);
        String localLepPath = classpath.resolvePath(TENANT_KEY, APP_NAME, PATH);

        assertEquals("classpath:/lep/custom/commons/functions/aggregation", envLepPath);
        assertEquals("classpath:/lep/custom/xm/commons/functions/aggregation", tenantLepPath);
        assertEquals("classpath:/lep/custom/xm/commons/functions/aggregation", localLepPath);
    }

    @Test
    public void testResolveLepForXmConfigScriptStorage() {
        var xmMsConfig = new XmMsConfigTenantScriptPathResolver();
        String envLepPath = xmMsConfig.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_ENVIRONMENT + PATH);
        String tenantLepPath = xmMsConfig.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_TENANT + PATH);
        String localLepPath = xmMsConfig.resolvePath(TENANT_KEY, APP_NAME, PATH);

        assertEquals("xm-ms-config:/config/tenants/commons/lep/commons/functions/aggregation", envLepPath);
        assertEquals("xm-ms-config:/config/tenants/XM/commons/lep/commons/functions/aggregation", tenantLepPath);
        assertEquals("xm-ms-config:/config/tenants/XM/activation/lep/commons/functions/aggregation", localLepPath);
    }

    @Test
    public void testResolveLepForFileScriptStorage() {
        var file = new FileTenantScriptPathResolver(FileSystemUtils.getAppHomeDir());
        String envLepPath = file.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_ENVIRONMENT + PATH);
        String tenantLepPath = file.resolvePath(TENANT_KEY, APP_NAME, URL_PREFIX_COMMONS_TENANT + PATH);
        String localLepPath = file.resolvePath(TENANT_KEY, APP_NAME, PATH);

        String basePath = FileSystemUtils.getCompleteFilePath();

        String expected = separatorsToSystem("/config/tenants/commons/lep/commons/functions/aggregation");
        String result = envLepPath.substring(basePath.length());

        assertEquals(expected, result);
        assertEquals(separatorsToSystem("/config/tenants/XM/commons/lep/commons/functions/aggregation"),
            tenantLepPath.substring(basePath.length()));
        assertEquals(separatorsToSystem("/config/tenants/XM/activation/lep/commons/functions/aggregation"),
            localLepPath.substring(basePath.length()));
    }
}
