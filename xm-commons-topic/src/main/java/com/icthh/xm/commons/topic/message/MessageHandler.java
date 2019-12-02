package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.topic.domain.TopicConfig;

public interface MessageHandler {

    void onMessage(String message, String tenant, TopicConfig topicConfig);
}
