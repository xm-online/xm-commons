package com.icthh.xm.commons.topic.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

@Getter
@RequiredArgsConstructor
public class ConsumerHolder {

    private final TopicConfig topicConfig;
    private final AbstractMessageListenerContainer container;
}
