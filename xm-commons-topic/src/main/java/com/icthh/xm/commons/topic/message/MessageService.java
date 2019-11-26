package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@Transactional
@LepService(group = "topic")
public class MessageService {

    @LogicExtensionPoint(value = "TopicMessage")
    public void onMessage(Map topicMessage) {
        log.error("No handlers for event {} found", topicMessage);
    }
}
