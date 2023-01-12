package com.icthh.xm.commons.topic.domain;

import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.Data;

@Data
public class DynamicConsumer {
    private TopicConfig config;
    private MessageHandler messageHandler;
}
