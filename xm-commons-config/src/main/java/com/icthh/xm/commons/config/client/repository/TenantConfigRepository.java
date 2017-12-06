package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.client.config.XmConfigAutoConfigration.XM_CONFIG_REST_TEMPLATE;
import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class TenantConfigRepository {

    public static final String URL = "/api/config/tenants/{tenantName}/";

    private final RestTemplate restTemplate;

    private final String applicationName;

    private final String xmConfigUrl;

    public TenantConfigRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                  @Value("${spring.application.name}") String applicationName,
                                  XmConfigProperties applicationProperties) {
        this.restTemplate = restTemplate;
        this.xmConfigUrl = applicationProperties.getXmConfigUrl();
        this.applicationName = applicationName;
    }

    public void createConfig(String tenantName, String path, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.postForEntity(getServiceConfigUrl() + path, entity, Void.class, tenantName);
    }

    public void updateConfig(String tenantName, String path, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.exchange(getServiceConfigUrl() + path, HttpMethod.PUT, entity, Void.class, tenantName);
    }

    public void deleteConfig(String tenantName, String path) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(getServiceConfigUrl() + path, HttpMethod.DELETE, entity, Void.class, tenantName);
    }

    public void createConfigFullPath(String tenantName, String fullPath, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.postForEntity(xmConfigUrl + fullPath, entity, Void.class, tenantName);
    }

    public void updateConfigFullPath(String tenantName, String fullPath, String content) {
        HttpEntity<String> entity = new HttpEntity<>(content, createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + fullPath, HttpMethod.PUT, entity, Void.class, tenantName);
    }

    public void deleteConfigFullPath(String tenantName, String fullPath) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(xmConfigUrl + fullPath, HttpMethod.DELETE, entity, Void.class, tenantName);
    }

    public String getConfigFullPath(String tenantName, String path) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        return  restTemplate.exchange(xmConfigUrl + path, HttpMethod.GET,
                                      entity, String.class, tenantName).getBody();
    }


    private String getServiceConfigUrl() {
        return xmConfigUrl + URL + applicationName;
    }


}
