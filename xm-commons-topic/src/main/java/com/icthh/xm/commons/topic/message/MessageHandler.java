package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import java.util.Map;

public interface MessageHandler {

    default void onMessage(String message, String tenant, TopicConfig topicConfig) {
    }

    default void onMessage(String message, String tenant, TopicConfig topicConfig, Map<String, byte[]> headers) {
        onMessage(message, tenant, topicConfig);
    }
}
