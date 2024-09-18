package com.icthh.xm.commons.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConsumersSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class TopicConfigurationService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenant";

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    private final String configPath;

    private final TopicDynamicConsumerConfiguration topicDynamicConsumerConfiguration;

    public TopicConfigurationService(@Value("${spring.application.name}") String appName,
                                     TopicDynamicConsumerConfiguration topicDynamicConsumerConfiguration) {
        this.configPath = "/config/tenants/{tenant}/" + appName + "/topic-consumers.yml";
        this.topicDynamicConsumerConfiguration = topicDynamicConsumerConfiguration;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    public Map<String, List<DynamicConsumer>> getTenantTopicConsumers() {
        return topicDynamicConsumerConfiguration.getTenantTopicConsumers();
    }

    private void refreshConfig(String updatedKey, String config) {
        String tenantKey = extractTenant(updatedKey);

        if (StringUtils.isEmpty(config)) {
            topicDynamicConsumerConfiguration.remove(tenantKey);
        } else {
            readSpec(updatedKey, config).ifPresentOrElse(spec -> {
                topicDynamicConsumerConfiguration.refreshConfig(spec.getTopics(), tenantKey);
            }, () -> log.warn("Skip processing of configuration: [{}]. Specification is null", updatedKey));
        }

        topicDynamicConsumerConfiguration.sendRefreshDynamicConsumersEvent(tenantKey);
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
    }

    private Optional<TopicConsumersSpec> readSpec(String updatedKey, String config) {
        try {
            return Optional.of(ymlMapper.readValue(config, TopicConsumersSpec.class));
        } catch (Exception e) {
            log.error("Error read topic specification from path: {}", updatedKey, e);
        }

        return Optional.empty();
    }
}
