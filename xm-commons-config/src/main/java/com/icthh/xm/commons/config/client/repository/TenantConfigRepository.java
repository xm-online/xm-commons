package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createJsonAuthHeaders;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createMultipartAuthHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TenantConfigRepository {

    static final String OLD_CONFIG_HASH = "oldConfigHash";

    private static final String TENANT_NAME = "tenantName";

    private static final String PATH_API = "/api";
    private static final String PATH_CONFIG = "/config";
    private static final String PATH_CONFIG_TENANT = PATH_CONFIG + "/tenants/{" + TENANT_NAME + "}/";
    private static final String URL = PATH_API + PATH_CONFIG_TENANT;

    private static final String MULTIPART_FILE_NAME = "files";

    private final RestTemplate restTemplate;

    private final String applicationName;

    private final String xmConfigUrl;

    public TenantConfigRepository(RestTemplate restTemplate,
                                  String applicationName,
                                  XmConfigProperties applicationProperties) {
        this.restTemplate = restTemplate;
        this.xmConfigUrl = applicationProperties.getXmConfigUrl();
        this.applicationName = applicationName;
    }

    public void createConfig(String tenantName, String path, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.postForEntity(getServiceConfigUrl() + path, entity, Void.class, tenantName);
    }

    public void createConfigs(String tenantName, List<Configuration> configurations) {

        List<NamedByteArrayResource> resources = configurations
            .stream()
            .map(this::toFullPath)
            .map(configuration -> toNamedResource(tenantName, configuration))
            .collect(Collectors.toList());

        postConfigAsMultipart(resources);

    }

    public void updateConfig(String tenantName, String path, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.exchange(getServiceConfigUrl() + path, HttpMethod.PUT, entity, Void.class, tenantName);
    }

    public void updateConfig(String tenantName, String path, String content, String oldConfigHash) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());

        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(TENANT_NAME, tenantName);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getServiceConfigUrl() + path)
                                                           .queryParam(OLD_CONFIG_HASH, oldConfigHash);

        restTemplate.exchange(builder.buildAndExpand(uriParams).toUri(), HttpMethod.PUT, entity, Void.class);
    }

    public void deleteConfig(String tenantName, String path) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(getServiceConfigUrl() + path, HttpMethod.DELETE, entity, Void.class, tenantName);
    }

    public void createConfigFullPath(String tenantName, String fullPath, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.postForEntity(xmConfigUrl + fullPath, entity, Void.class, tenantName);
    }

    public void createConfigsFullPath(String tenantName, List<Configuration> configurations) {

        List<NamedByteArrayResource> resources = configurations
            .stream()
            .map(configuration -> toNamedResource(tenantName, configuration))
            .collect(Collectors.toList());

        postConfigAsMultipart(resources);
    }

    public void updateConfigFullPath(String tenantName, String fullPath, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + fullPath, HttpMethod.PUT, entity, Void.class, tenantName);
    }

    public void updateConfigFullPath(String tenantName, String fullPath, String content, String oldConfigHash) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());

        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(TENANT_NAME, tenantName);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(xmConfigUrl + fullPath)
                                                           .queryParam(OLD_CONFIG_HASH, oldConfigHash);

        restTemplate.exchange(builder.buildAndExpand(uriParams).toUri(), HttpMethod.PUT, entity, Void.class);
    }

    public void deleteConfigFullPath(String tenantName, String fullPath) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + fullPath, HttpMethod.DELETE, entity, Void.class, tenantName);
    }

    public void deleteConfigFullPaths(String tenantName, List<String> fullPath) {
        List<String> tenantResolved = fullPath.stream()
                                              .map(s -> resolveTenantName(s, tenantName))
                                              .collect(Collectors.toList());
        HttpEntity<List<String>> entity = new HttpEntity<>(tenantResolved, createJsonAuthHeaders());
        restTemplate.exchange(xmConfigUrl + URL, HttpMethod.DELETE, entity, Void.class, tenantName);
    }

    public String getConfigFullPath(String tenantName, String path) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        return restTemplate.exchange(xmConfigUrl + path, HttpMethod.GET, entity, String.class, tenantName).getBody();
    }

    private String getServiceConfigUrl() {
        return xmConfigUrl + URL + applicationName;
    }

    private Configuration toFullPath(Configuration configuration) {
        return new Configuration(Paths.get(PATH_CONFIG_TENANT, applicationName, configuration.getPath()).toString(),
                                 configuration.getContent());
    }

    private void postConfigAsMultipart(final List<NamedByteArrayResource> resources) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.addAll(MULTIPART_FILE_NAME, resources);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, createMultipartAuthHeaders());

        restTemplate.postForEntity(xmConfigUrl + PATH_API + PATH_CONFIG, entity, Void.class);
    }

    private String resolveTenantName(String path, String tenantName) {
        return path.replaceAll("\\{tenantName}", tenantName);
    }

    private NamedByteArrayResource toNamedResource(String tenantName, Configuration configuration) {
        return new NamedByteArrayResource(configuration.getContent().getBytes(),
                                          resolveTenantName(configuration.getPath(), tenantName));
    }

}
