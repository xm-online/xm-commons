package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@IgnoreLogginAspect
@LepService(group = "topic")
public class MessageService {

    private MessageService self;

    @Autowired
    public void setSelf(@Lazy MessageService self) {
        this.self = self;
    }

    @LogicExtensionPoint(value = "OnMessage", resolver = MessageTypeKeyResolver.class)
    public void onMessage(String topicMessage, TopicConfig topicConfig) {
        log.error("No handlers for event: {} and topic config: {} found", topicMessage, topicConfig);
    }

    @LogicExtensionPoint(value = "OnMessageWithHeaders", resolver = MessageTypeKeyResolver.class)
    public void onMessageWithHeaders(String topicMessage, TopicConfig topicConfig, Map<String, byte[]> headers) {
        self.onMessage(topicMessage, topicConfig);
    }
}
