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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncKafkaTransport implements Transport {

    private static final String TOPIC_FORMAT = "event.%s.%s";

    private final KafkaTemplateService kafkaTemplateService;
    private final ObjectMapper objectMapper;
    private final KafkaTransactionSynchronizationAdapterService kafkaTransactionSynchronizationAdapterService;

    @Override
    public void send(DomainEvent event) {
        Consumer<DomainEvent> domainEventConsumer = sendMessageConsumer();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            kafkaTransactionSynchronizationAdapterService.send(event, domainEventConsumer);
            return;
        }

        domainEventConsumer.accept(event);
    }

    private Consumer<DomainEvent> sendMessageConsumer() {
        return (domainEvent) -> {
            String topic = prepareTopicName(domainEvent);
            String data = toJson(domainEvent);
            kafkaTemplateService.send(topic, data);
            log.info("Send event to kafka topic = {}", topic);
        };
    }

    @SneakyThrows
    private String toJson(DomainEvent domainEvent) {
        return objectMapper.writeValueAsString(domainEvent);
    }

    private String prepareTopicName(DomainEvent event) {
        if (StringUtils.isBlank(event.getTenant()) ||
            StringUtils.isBlank(event.getSource())) {
            throw new IllegalArgumentException("Empty fields in domain event!");
        }

        return String.format(TOPIC_FORMAT, event.getTenant(), event.getSource()).toLowerCase();
    }
}
