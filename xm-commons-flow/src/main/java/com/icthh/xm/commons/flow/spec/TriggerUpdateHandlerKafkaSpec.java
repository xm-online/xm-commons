package com.icthh.xm.commons.flow.spec;

import com.icthh.xm.commons.config.client.api.refreshable.SingleFileAbstractRefreshableConfiguration;
import com.icthh.xm.commons.flow.service.trigger.TriggerProcessor;
import com.icthh.xm.commons.flow.service.trigger.TriggerUpdateHandler;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TriggerUpdateHandlerKafkaSpec extends SingleFileAbstractRefreshableConfiguration<TopicConfig>
    implements DynamicConsumerConfiguration {

    public static final String FLOW_TRIGGER_UPDATE_TOPIC_NAME = "flow_trigger_update";
    private final Map<String, TopicConfig> configuration = new ConcurrentHashMap<>();

    private final TriggerUpdateHandler triggerUpdateHandler;

    public TriggerUpdateHandlerKafkaSpec(@Value("${spring.application.name}") String appName,
                                         TenantContextHolder tenantContextHolder,
                                         TriggerUpdateHandler triggerUpdateHandler) {
        super(appName, tenantContextHolder);
        this.triggerUpdateHandler = triggerUpdateHandler;
    }

    @Override
    public Class<TopicConfig> configFileClass() {
        return TopicConfig.class;
    }

    @Override
    public String folder() {
        return "/flow";
    }

    @Override
    public String configName() {
        return "trigger-update-kafka-settings.yml";
    }

    @Override
    public void onUpdate(String tenantName, TopicConfig configuration) {
        configuration.setKey(FLOW_TRIGGER_UPDATE_TOPIC_NAME);
        configuration.setTopicName(tenantName + "." + FLOW_TRIGGER_UPDATE_TOPIC_NAME);
        configuration.setTypeKey(FLOW_TRIGGER_UPDATE_TOPIC_NAME);
        this.configuration.put(tenantName, configuration);
    }

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        if (this.configuration.containsKey(tenantKey)) {
            TopicConfig topicConfig = this.configuration.get(tenantKey);
            DynamicConsumer dynamicConsumer = new DynamicConsumer();
            dynamicConsumer.setConfig(topicConfig);
            dynamicConsumer.setMessageHandler((message, tenant, config) -> triggerUpdateHandler.processTriggerUpdate(tenantKey, message));
            return List.of(dynamicConsumer);
        }
        return List.of();
    }
}
