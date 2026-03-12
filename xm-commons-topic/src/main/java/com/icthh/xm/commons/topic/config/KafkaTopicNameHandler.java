package com.icthh.xm.commons.topic.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTopicNameHandler {

    /**
     * Flag to enable tenant-based prefix for ALL Kafka topic names.
     * When enabled, topics will be prefixed with "tenant_topic_${tenant}_${topicName}" pattern.
     * This includes dynamic topics, system topics, etc.
     * Note: config topics are excluded from prefixing.
     * The prefix is applied on both producer and consumer sides.
     */

    @Value("${application.kafka.addTenantPrefix:false}")
    private Boolean addTenantPrefix = false;

    /**
     * Get the topic name with tenant prefix if enabled.
     * Config topics (config_queue, config_topic) are excluded from prefixing.
     *
     * @param topicName the original topic name
     * @param tenant the tenant key
     * @return the topic name with tenant prefix applied if enabled
     */
    public String getPrefixedTopicName(String topicName, String tenant) {
        if (!addTenantPrefix || StringUtils.isBlank(topicName) || StringUtils.isBlank(tenant)) {
            return topicName;
        }

        // Exclude config topics from prefixing
        if (isConfigTopic(topicName)) {
            return topicName;
        }

        return "tenant_topic_" + tenant + "_" + topicName;
    }

    /**
     * Check if the topic name is a config topic.
     * Config topics equalsIgnoreCase with "config_topic" or "config_queue".
     *
     * @param topicName the topic name to check
     * @return true if it's a config topic, false otherwise
     */
    private boolean isConfigTopic(String topicName) {
        return topicName != null &&
               (topicName.equalsIgnoreCase("config_topic") ||
                       topicName.equalsIgnoreCase("config_queue"));
    }
}
