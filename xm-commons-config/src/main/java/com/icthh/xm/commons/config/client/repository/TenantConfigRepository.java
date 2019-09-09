package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createJsonAuthHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createMultipartAuthHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Repository to manage tenant related files inside ms-config.
 *
 * Service prevents changing files not outside /config/tenants/{tenantName} directory (inclusive).
 */
@Slf4j
public class TenantConfigRepository {

    static final String OLD_CONFIG_HASH = "oldConfigHash";

    private static final String TENANT_NAME = "tenantName";

    private static final String PATH_API = "/api";
    private static final String PATH_CONFIG = "/config";

    public static final String PATH_CONFIG_TENANT = PATH_CONFIG + "/tenants/{" + TENANT_NAME + "}/";
    public static final String PATH_API_CONFIG_TENANT = PATH_API + PATH_CONFIG_TENANT;

    private static final String MULTIPART_FILE_NAME = "files";

    private static final String TENANT_PATH_PATTERN = "/**/config/tenants/{tenantName:[A-Z0-9]+|\\{tenantName\\}}/**";
    private static final String TENANT_NAME_PATTERN = "[A-Z0-9]+";
    private static final String TENANT_NAME_REPLACE = "\\{" + TENANT_NAME + "}";

    private final RestTemplate restTemplate;

    private final String applicationName;

    private final String xmConfigUrl;

    private final AntPathMatcher matcher = new AntPathMatcher();

    public TenantConfigRepository(RestTemplate restTemplate,
                                  String applicationName,
                                  XmConfigProperties applicationProperties) {
        this.restTemplate = restTemplate;
        this.xmConfigUrl = applicationProperties.getXmConfigUrl();
        this.applicationName = applicationName;
    }

    public void createConfig(String tenantName, String path, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        exchangePost(tenantName, getServiceConfigUrl() + path, entity);
    }

    public void createConfigs(String tenantName, List<Configuration> configurations) {
        String upperTenantName = tenantName.toUpperCase();

        assertTenantNameValid(upperTenantName);

        List<NamedByteArrayResource> resources = configurations
            .stream()
            .map(this::toFullPath)
            .map(configuration -> toNamedResource(upperTenantName, configuration))
            .collect(Collectors.toList());

        exchangePostMultipart(resources);
    }

    public void updateConfig(String tenantName, String path, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        exchangePut(tenantName, getServiceConfigUrl() + path, entity);
    }

    public void updateConfig(String tenantName, String path, String content, String oldConfigHash) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        String pathWithHash = toUrlWithOldHash(tenantName, getServiceConfigUrl() + path, oldConfigHash);
        exchangePut(null, pathWithHash, entity);
    }

    public void deleteConfig(String tenantName, String path) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        exchangeDelete(tenantName, getServiceConfigUrl() + path, entity);
    }

    public void createConfigFullPath(String tenantName, String fullPath, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        exchangePost(tenantName, xmConfigUrl + fullPath, entity);
    }

    public void createConfigsFullPath(String tenantName, List<Configuration> configurations) {

        String upperTenantName = tenantName.toUpperCase();

        assertTenantNameValid(upperTenantName);

        List<NamedByteArrayResource> resources = configurations
            .stream()
            .map(configuration -> toNamedResource(upperTenantName, configuration))
            .collect(Collectors.toList());

        exchangePostMultipart(resources);
    }

    public void updateConfigFullPath(String tenantName, String fullPath, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        exchangePut(tenantName, xmConfigUrl + fullPath, entity);
    }

    public void updateConfigFullPath(String tenantName, String fullPath, String content, String oldConfigHash) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        String path = toUrlWithOldHash(tenantName, xmConfigUrl + fullPath, oldConfigHash);
        exchangePut(null, path, entity);
    }

    public void deleteConfigFullPath(String tenantName, String fullPath) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        exchangeDelete(tenantName, xmConfigUrl + fullPath, entity);
    }

    public void deleteConfigFullPaths(String tenantName, List<String> fullPath) {
        List<String> tenantResolved = fullPath.stream()
                                              .map(s -> resolveTenantName(s, tenantName))
                                              .collect(Collectors.toList());
        HttpEntity<List<String>> entity = new HttpEntity<>(tenantResolved, createJsonAuthHeaders());
        exchangeDelete(tenantName, xmConfigUrl + PATH_API_CONFIG_TENANT, entity);
    }

    public String getConfigFullPath(String tenantName, String path) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        return exchangeGet(tenantName, xmConfigUrl + path, entity).getBody();
    }

    private ResponseEntity<String> exchangeGet(final String tenantName, final String path,
                                               final HttpEntity<String> entity) {
        return exchange(path, HttpMethod.GET, entity, String.class, tenantName);
    }

    private void exchangePut(final String tenantName, final String path,
                             final HttpEntity<String> entity) {
        exchange(path, HttpMethod.PUT, entity, tenantName);
    }

    private void exchangeDelete(final String tenantName, final String path, final HttpEntity<?> entity) {
        exchange(path, HttpMethod.DELETE, entity, tenantName);
    }

    private void exchangePost(final String tenantName, final String path, final HttpEntity<?> entity) {
        exchange(path, HttpMethod.POST, entity, tenantName);
    }

    private void exchange(String path, HttpMethod method, HttpEntity<?> entity, String tenantName) {
        exchange(path, method, entity, Void.class, tenantName);
    }

    private <T> ResponseEntity<T> exchange(String path,
                                           HttpMethod method,
                                           HttpEntity<?> entity,
                                           Class<T> respClass,
                                           String tenantName) {

        assertPathInsideTenant(path);

        if (tenantName != null) {
            String upperTenantName = tenantName.toUpperCase();
            assertTenantNameValid(upperTenantName);
            return restTemplate.exchange(path, method, entity, respClass, upperTenantName);
        }
        return restTemplate.exchange(path, method, entity, respClass);
    }

    private void exchangePostMultipart(final List<NamedByteArrayResource> resources) {
        resources.forEach(namedByteArrayResource -> assertPathInsideTenant(namedByteArrayResource.getFilename()));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.addAll(MULTIPART_FILE_NAME, resources);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, createMultipartAuthHeaders());
        restTemplate.exchange(xmConfigUrl + PATH_API + PATH_CONFIG, HttpMethod.POST, entity, Void.class);
    }

    private String toUrlWithOldHash(final String tenantName, final String path, final String oldConfigHash) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(TENANT_NAME, tenantName.toUpperCase());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path)
                                                           .queryParam(OLD_CONFIG_HASH, oldConfigHash);

        return builder.buildAndExpand(uriParams).toUri().toString();
    }

    void assertTenantNameValid(String tenantName) {
        Objects.requireNonNull(tenantName, "tenantName can not be null");
        if (!tenantName.matches(TENANT_NAME_PATTERN)) {
            throw new IllegalArgumentException("Tenant name has wrong format: " + tenantName);
        }
    }

    @SneakyThrows
    void assertPathInsideTenant(String path) {

        Objects.requireNonNull(path, "path can not be null");

        String contextPath = path;
        if (path.startsWith("http")) {
            contextPath = new URL(path).getPath();
        }

        if (!matcher.match(TENANT_PATH_PATTERN, contextPath)) {
            throw new IllegalArgumentException("Execution is not allowed for path: " + contextPath);
        }
    }

    private String getServiceConfigUrl() {
        return xmConfigUrl + PATH_API_CONFIG_TENANT + applicationName;
    }

    private Configuration toFullPath(Configuration configuration) {
        return new Configuration(Paths.get(PATH_CONFIG_TENANT, applicationName, configuration.getPath()).toString(),
                                 configuration.getContent());
    }

    private String resolveTenantName(String path, String tenantName) {
        return path.replaceAll(TENANT_NAME_REPLACE, tenantName.toUpperCase());
    }

    private NamedByteArrayResource toNamedResource(String tenantName, Configuration configuration) {
        return new NamedByteArrayResource(configuration.getContent().getBytes(),
                                          resolveTenantName(configuration.getPath(), tenantName));
    }

}
