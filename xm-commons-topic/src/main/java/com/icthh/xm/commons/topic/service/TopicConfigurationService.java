package com.icthh.xm.commons.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TopicConfigurationService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    private final String configPath;
    private final TopicManagerService topicManagerService;
    private final DynamicConsumerConfigurationService dynamicConsumerConfigurationService;

    @Getter
    private Map<String, Map<String, ConsumerHolder>> tenantTopicConsumers = new ConcurrentHashMap<>();

    public TopicConfigurationService(@Value("${spring.application.name}") String appName,
                                     TopicManagerService topicManagerService,
                                     DynamicConsumerConfigurationService dynamicConsumerConfigurationService) {
        this.configPath = "/config/tenants/{tenant}/" + appName + "/topic-consumers.yml";
        this.topicManagerService = topicManagerService;
        this.dynamicConsumerConfigurationService = dynamicConsumerConfigurationService;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        if (isListeningConfiguration(configKey)) {
            refreshConfig(configKey, configValue);
        }
    }

    private void refreshConfig(String updatedKey, String config) {
        String tenantKey = extractTenant(updatedKey);
        Map<String, ConsumerHolder> existingConsumers = getTenantConsumers(tenantKey);

        if (StringUtils.isEmpty(config)) {
            topicManagerService.stopAllTenantConsumers(tenantKey, existingConsumers);
            tenantTopicConsumers.remove(tenantKey);
            return;
        }
        TopicConsumersSpec spec = readSpec(updatedKey, config);
        if (spec == null) {
            log.warn("Skip processing of configuration: [{}]. Specification is null", updatedKey);
            return;
        }
        List<TopicConfig> forUpdate = spec.getTopics();

        //start and update consumers
        forUpdate.forEach(topicConfig -> topicManagerService.processTopicConfig(tenantKey, topicConfig, existingConsumers));

        //remove old consumers
        topicManagerService.removeOldConsumers(tenantKey, forUpdate, existingConsumers);

        tenantTopicConsumers.put(tenantKey, existingConsumers);

        dynamicConsumerConfigurationService.startDynamicConsumers(tenantKey);
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
    }

    private TopicConsumersSpec readSpec(String updatedKey, String config) {
        TopicConsumersSpec spec = null;
        try {
            spec = ymlMapper.readValue(config, TopicConsumersSpec.class);
        } catch (Exception e) {
            log.error("Error read topic specification from path: {}", updatedKey, e);
        }
        return spec;
    }

    private Map<String, ConsumerHolder> getTenantConsumers(String tenantKey) {
        if (tenantTopicConsumers.containsKey(tenantKey)) {
            return tenantTopicConsumers.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }
}
