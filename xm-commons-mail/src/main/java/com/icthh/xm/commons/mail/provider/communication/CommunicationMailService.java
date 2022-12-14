package com.icthh.xm.commons.mail.provider.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationMailService {

    private final KafkaTemplateService kafkaTemplateService;
    private final TenantContextHolder tenantContextHolder;
    private final ObjectMapper objectMapper;
    @Value("${application.kafka-communication-queue:'%s_communication_queue'}")
    private String topicName;

    public void sendEmailEvent(CommunicationMessage message) {
        String serializedMessage = convertToString(message);
        sendMessage(serializedMessage);
    }

    public CommunicationMessage addTemplateModelToMessage(CommunicationMessage message, Map<String, Object> model) {
        String serializedModel = convertToString(model);
        return message.setTemplateModel(serializedModel);
    }

    private void sendMessage(String content) {
        String tenantName = tenantContextHolder.getTenantKey();
        String topicFullName = String.format(topicName, tenantName);
        log.info("Sending communication message event to kafka-topic = '{}', data = '{}'", topicFullName, content);
        kafkaTemplateService.send(topicFullName, content);
    }

    @SneakyThrows
    private String convertToString(Object object) {
        return objectMapper.writeValueAsString(object);
    }
}
