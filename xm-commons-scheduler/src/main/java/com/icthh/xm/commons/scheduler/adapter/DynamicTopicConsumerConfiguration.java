package com.icthh.xm.commons.scheduler.adapter;

import com.icthh.xm.commons.scheduler.service.SchedulerEventHandlerFacade;
import com.icthh.xm.commons.scheduler.service.SchedulerEventService;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.AbstractDynamicConsumerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.upperCase;

@Slf4j
public class DynamicTopicConsumerConfiguration extends AbstractDynamicConsumerConfiguration {

    private static final String PREFIX = "scheduler_";
    private static final String DELIMITER = "_";
    private static final String GENERAL_GROUP = "GENERALGROUP";
    private static final String TOPIC = "_topic";
    private static final String QUEUE = "_queue";
    private static final String AUTO_OFFSET_RESET_LATEST = "latest";
    private static final String AUTO_OFFSET_RESET_EARLIEST = "earliest";

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.scheduler-config.task-back-off-initial-interval:1000}")
    private long backOffInitialInterval;

    @Value("${application.scheduler-config.task-back-off-max-interval:60000}")
    private int backOffMaxInterval;

    @Value("${application.kafka-metadata-max-age:60000}")
    private int kafkaMetadataMaxAge;

    private final Map<String, List<DynamicConsumer>> dynamicConsumersByTenant;
    private final SchedulerEventHandlerFacade schedulerEventHandlerFacade;

    public DynamicTopicConsumerConfiguration(SchedulerEventService schedulerEventService,
                                             ApplicationEventPublisher applicationEventPublisher) {
        super(applicationEventPublisher);
        this.dynamicConsumersByTenant = new ConcurrentHashMap<>();
        this.schedulerEventHandlerFacade = new SchedulerEventHandlerFacade(schedulerEventService);
    }

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        return dynamicConsumersByTenant.getOrDefault(getTenantMapKey(tenantKey), new ArrayList<>());
    }

    public void buildDynamicConsumers(String tenantName) {
        try {
            String tenant = lowerCase(tenantName);
            String tenantKey = upperCase(tenantName);
            String id = randomUUID().toString();

            createDynamicConsumer(PREFIX + tenant + QUEUE, GENERAL_GROUP, tenantKey, AUTO_OFFSET_RESET_EARLIEST);
            createDynamicConsumer(PREFIX + tenant + TOPIC, id, tenantKey, AUTO_OFFSET_RESET_LATEST);
            createDynamicConsumer(PREFIX + tenant + DELIMITER + appName + QUEUE, appName, tenantKey, AUTO_OFFSET_RESET_EARLIEST);
            createDynamicConsumer(PREFIX + tenant + DELIMITER + appName + TOPIC, id, tenantKey, AUTO_OFFSET_RESET_LATEST);

        } catch (Exception e) {
            log.error("Error create scheduler channels for tenant " + tenantName, e);
            throw e;
        }
    }

    private void createDynamicConsumer(String chanelName, String consumerGroup, String tenantName, String startOffset) {
        DynamicConsumer dynamicConsumer = new DynamicConsumer();
        dynamicConsumer.setConfig(buildTopicConfig(chanelName, consumerGroup, startOffset));
        dynamicConsumer.setMessageHandler(schedulerEventHandlerFacade);

        String tenantMapKey = getTenantMapKey(tenantName);
        dynamicConsumersByTenant.computeIfAbsent(tenantMapKey, v -> new ArrayList<>()).add(dynamicConsumer);
    }

    private TopicConfig buildTopicConfig(String chanelName, String consumerGroup, String startOffset) {
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setKey(chanelName);
        topicConfig.setTypeKey(chanelName);
        topicConfig.setTopicName(chanelName);
        topicConfig.setRetriesCount(Integer.MAX_VALUE);
        topicConfig.setBackOffPeriod(backOffInitialInterval);
        topicConfig.setMaxPollInterval(backOffMaxInterval);
        topicConfig.setGroupId(consumerGroup);
        topicConfig.setAutoOffsetReset(startOffset);
        topicConfig.setMetadataMaxAge(String.valueOf(kafkaMetadataMaxAge));
        return topicConfig;
    }

    private String getTenantMapKey(String tenantName) {
        return tenantName != null ? tenantName.toLowerCase() : null;
    }
}
