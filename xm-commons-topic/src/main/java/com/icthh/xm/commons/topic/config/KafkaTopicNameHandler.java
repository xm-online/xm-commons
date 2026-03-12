package com.icthh.xm.commons.topic.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTopicNameHandler {

    @Value("${application.kafka.addTenantPrefix:false}")
    private Boolean addTenantPrefix = false;

    public String getPrefixedTopicName(String topicName, String tenant) {
        if (!addTenantPrefix || StringUtils.isBlank(topicName) || StringUtils.isBlank(tenant)) {
            return topicName;
        }

        return "tenant_topic_" + tenant + "_" + topicName;
    }
}
