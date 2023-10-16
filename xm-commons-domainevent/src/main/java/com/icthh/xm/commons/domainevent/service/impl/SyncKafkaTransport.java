package com.icthh.xm.commons.domainevent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.Transport;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

/**
 *  Example of KafkaTransactionalConfig implementation. Should be implemented in main microservice to avoid:
 *  Caused by: java.lang.IllegalAccessError: class com.icthh.xm.commons.domainevent.config.KafkaTransactionalConfig$1$$EnhancerBySpringCGLIB$$79a220b3
 *  cannot access its superclass com.icthh.xm.commons.domainevent.config.KafkaTransactionalConfig$1
 *  (com.icthh.xm.commons.domainevent.config.KafkaTransactionalConfig$1$$EnhancerBySpringCGLIB$$79a220b3 is in unnamed module
 *  of loader org.springframework.boot.devtools.restart.classloader.RestartClassLoader @24163a8c;
 *  com.icthh.xm.commons.domainevent.config.KafkaTransactionalConfig$1 is in unnamed module of loader 'app')
 *
 *    public KafkaTransactionalConfig(ConfigurableListableBeanFactory factory) {
 *         factory.registerScope("transaction", new SimpleTransactionScope());
 *     }
 *
 *     @Bean
 *     default KafkaTransactionSynchronizationAdapterService kafkaTransactionSynchronizationAdapterService(ApplicationContext context) {
 *         return new KafkaTransactionSynchronizationAdapterService() {
 *             @Override
 *             public KafkaTransactionSynchronizationAdapter getKafkaTransactionSynchronizationAdapter() {
 *                 return context.getBean(KafkaTransactionSynchronizationAdapter.class);
 *             }
 *         };
 *     }
 * **/

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncKafkaTransport implements Transport {

    private static final String TOPIC_FORMAT = "event.%s.%s";

    private final KafkaTemplateService kafkaTemplateService;
    private final ObjectMapper objectMapper;
    private final KafkaTransactionSynchronizationAdapterService kafkaTransactionSynchronizationAdapterService;

    @LoggingAspectConfig(inputDetails = false)
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
