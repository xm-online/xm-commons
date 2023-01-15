package com.icthh.xm.commons.domainevent.service.imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.Transport;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncKafkaTransport implements Transport {

    private static final String TOPIC_FORMAT = "event.%s.%s";

    private final KafkaTemplateService kafkaTemplateService;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void send(DomainEvent event) {
        String topic = prepareTopicName(event);
        String data = objectMapper.writeValueAsString(event);
        kafkaTemplateService.send(topic, data);
        log.info("Send event to kafka topic = {}", topic);
    }

    private String prepareTopicName(DomainEvent event) {
        if (StringUtils.isBlank(event.getTenant()) ||
            StringUtils.isBlank(event.getSource())) {
            throw new IllegalArgumentException("Empty fields in domain event!");
        }

        return String.format(TOPIC_FORMAT, event.getTenant(), event.getSource()).toLowerCase();
    }
}
