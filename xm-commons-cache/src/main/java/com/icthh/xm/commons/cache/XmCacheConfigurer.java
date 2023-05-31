package com.icthh.xm.commons.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.cache.config.XmCacheConfig;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.AntPathMatcher;

import java.util.List;


@Slf4j
public class XmCacheConfigurer implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";
    private final String configPath;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    public XmCacheConfigurer(@Value("${spring.application.name}") String appName,
                             ApplicationEventPublisher applicationEventPublisher) {
        this.configPath = "/config/tenants/{tenant}/" + appName + "/cache.yml";
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        if (isListeningConfiguration(updatedKey)) {
            refreshCaches(updatedKey, config);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    private void refreshCaches(String updatedKey, String config) {
        if (StringUtils.isEmpty(config)) {
            return;
        }
        String tenantKey = extractTenant(updatedKey);
        List<XmCacheConfig.XmCacheConfiguration> items = readConfig(updatedKey, config);
        InitCachesEvent event = new InitCachesEvent(this, tenantKey, items);
        applicationEventPublisher.publishEvent(event);
    }

    protected List<XmCacheConfig.XmCacheConfiguration> readConfig(String updatedKey, String config) {
        List<XmCacheConfig.XmCacheConfiguration> cfg = List.of();
        try {
            cfg = ymlMapper.readValue(config, new TypeReference<List<XmCacheConfig.XmCacheConfiguration>>() {});
        } catch (Exception e) {
            log.error("Error reading event publisher config from path: {}", updatedKey, e);
        }
        return cfg;
    }

    protected String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
    }
}
