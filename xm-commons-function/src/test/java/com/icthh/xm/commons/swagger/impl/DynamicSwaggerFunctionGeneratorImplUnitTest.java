package com.icthh.xm.commons.swagger.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFile;
import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFunctionApiSpecByFile;
import static com.icthh.xm.commons.utils.SwaggerTestUtils.readExpected;
import static com.icthh.xm.commons.utils.SwaggerTestUtils.toYml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicSwaggerFunctionGeneratorImplUnitTest {

    public static final String APP_NAME_TENANT = "function";
    public static final String TEST_TENANT = "TEST_TENANT";
    public static final String SWAGGER_CONFIG_PATH = "/config/tenants/TEST_TENANT/function/swagger.yml";
    public static final String TEST_BASE_URL = "https://xm.domain.com:8080";

    private TenantContextHolder tenantContextHolder;
    private DynamicSwaggerRefreshableConfiguration dynamicSwaggerConfigService;
    private FunctionApiSpecConfiguration functionApiSpecConfiguration;
    private DynamicSwaggerFunctionGeneratorImpl dynamicSwaggerFunctionGenerator;

    @BeforeEach
    public void setUp() {
        tenantContextHolder = new DefaultTenantContextHolder();
        tenantContextHolder.getPrivilegedContext().setTenant(new PlainTenant(TenantKey.valueOf(TEST_TENANT)));

        dynamicSwaggerConfigService = new DynamicSwaggerRefreshableConfiguration(APP_NAME_TENANT, tenantContextHolder);

        functionApiSpecConfiguration = mock(FunctionApiSpecConfiguration.class);

        when(functionApiSpecConfiguration.getTenantSpecifications(TEST_TENANT)).thenReturn(Map.of(
            "/config/tenants/TEST_TENANT/function/functions/swagger-functions.yml", loadFunctionApiSpecByFile("swagger-functions")
        ));

        dynamicSwaggerFunctionGenerator = new DynamicSwaggerFunctionGeneratorImpl(
            APP_NAME_TENANT,
            dynamicSwaggerConfigService,
            functionApiSpecConfiguration,
            tenantContextHolder
        );
    }

    @AfterEach
    public void down() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testGenerateSwagger() {
        dynamicSwaggerConfigService.onRefresh(
            "/config/tenants/TEST_TENANT/function/test-swagger.yml",
            null
        );

        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);
        var expected = readExpected("config/swagger/expected-default.yml");

        assertThat(toYml(expected)).isEqualTo(toYml(swagger));
    }

    @Test
    public void testOverrideConfiguration() {
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, loadFile("config/swagger/test-swagger.yml"));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertEquals("4.5.0", swagger.getInfo().getVersion());
        assertEquals("Test swagger", swagger.getInfo().getTitle());
        assertEquals("https://test-env", swagger.getServers().get(0).getUrl());
        assertEquals("https://dev-env", swagger.getServers().get(1).getUrl());
        assertEquals(2, swagger.getServers().size());
        assertEquals(2, swagger.getTags().size());
        assertEquals("External functions", swagger.getTags().get(0).getDescription());
        assertEquals("external", swagger.getTags().get(0).getName());
        assertEquals("Test functions", swagger.getTags().get(1).getDescription());
        assertEquals("test", swagger.getTags().get(1).getName());

    }

    @Test
    public void testExcludeInclude() {
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, loadFile("config/swagger/test-swagger.yml"));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/folder/v1/TestName", List.of("post", "get"),
            "/function/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete"),
            "/function/api/functions/relative/path/{pathVariable}/other/{otherPathVariable}/{notDefinedVariable}/etc",
            List.of("post", "get", "delete", "put"),
            "/function/api/functions/folder/{variable}/StructureFunction", List.of("post", "get")
        ));
    }

    @Test
    public void testExcludeInclude_includeTag() {
        DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
        config.setIncludeTags(List.of("test"));
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, toYml(config));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete"),
            "/function/api/functions/relative/path/{pathVariable}/other/{otherPathVariable}/{notDefinedVariable}/etc",
            List.of("post", "get", "delete", "put")
        ));
    }

    @Test
    public void testExcludeInclude_excludeTag() {
        DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
        config.setExcludeTags(List.of("internal"));
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, toYml(config));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/folder/v1/TestName", List.of("post", "get"),
            "/function/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete"),
            "/function/api/functions/folder/{variable}/StructureFunction", List.of("post", "get")
        ));
    }

    @Test
    public void testExcludeInclude_includeAndExcludeTag() {
        DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
        config.setIncludeTags(List.of("test"));
        config.setExcludeTags(List.of("internal"));
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, toYml(config));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete")
        ));
    }

    @Test
    public void testExcludeInclude_includeAndMultipleExcludeTag() {
        DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
        config.setIncludeTags(List.of("test"));
        config.setExcludeTags(List.of("internal", "duplicatePathExclude"));
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, toYml(config));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/check/different/key/with/same/path", List.of("put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete")
        ));
    }

    @Test
    public void testExcludeInclude_multipleIncludeAndMultipleExcludeTag() {
        DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
        config.setIncludeTags(List.of("test", "external"));
        config.setExcludeTags(List.of("internal", "duplicatePathExclude"));
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, toYml(config));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/folder/v1/TestName", List.of("post", "get"),
            "/function/api/functions/check/different/key/with/same/path", List.of("put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete"),
            "/function/api/functions/folder/{variable}/StructureFunction", List.of("post", "get")
        ));
    }

    @Test
    public void testExcludeInclude_excludeKeyPatterns() {
        DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
        config.setIncludeTags(List.of("test", "external"));
        config.setExcludeTags(List.of("internal", "duplicatePathExclude"));
        config.setExcludeKeyPatterns(List.of("folder/v1/.*"));
        dynamicSwaggerConfigService.onRefresh(SWAGGER_CONFIG_PATH, toYml(config));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger(TEST_BASE_URL);

        assertPaths(swagger, Map.of(
            "/function/api/functions/check/different/key/with/same/path", List.of("put"),
            "/function/api/functions/NameFromKeyWrappedResultInReturn", List.of("post", "delete"),
            "/function/api/functions/folder/{variable}/StructureFunction", List.of("post", "get")
        ));
    }

    private void assertPaths(SwaggerModel swagger, Map<String, List<String>> paths) {
        Set<String> actualPaths = swagger.getPaths().keySet();
        assertThat(actualPaths).isEqualTo(paths.keySet());
        paths.forEach((path, methods) -> {
            assertThat(swagger.getPaths().get(path).keySet()).isEqualTo(new HashSet<>(methods));
        });
    }
}
