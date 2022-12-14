package com.icthh.xm.commons.mail.provider.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationMailService {

    private final KafkaTemplateService kafkaTemplateService;
    private final TenantContextHolder tenantContextHolder;
    @Value("${application.kafka-communication-queue:'%s_communication_queue'}")
    private String topicName;
    private ObjectMapper objectMapper = new ObjectMapper();

    public void sendEmailEvent(CommunicationMessage message, TemplateModel templateModel) {
        String convertedTemplateModel = serializeObjectToString(templateModel);
        Optional.of(message)
            .map(it -> it.setTemplateModel(convertedTemplateModel))
            .map(this::serializeObjectToString)
            .ifPresent(this::sendMessage);
    }

    private void sendMessage(String content) {
        if (!StringUtils.isBlank(content)) {
            String tenantName = tenantContextHolder.getTenantKey();
            String topicFullName = String.format(topicName, tenantName);
            log.info("Sending communication message event to kafka-topic = '{}', data = '{}'", topicFullName, content);
            kafkaTemplateService.send(topicFullName, content);
        }
    }

    private String serializeObjectToString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error while serializing object: {}", object, e);
        }

        return null;
    }
}
