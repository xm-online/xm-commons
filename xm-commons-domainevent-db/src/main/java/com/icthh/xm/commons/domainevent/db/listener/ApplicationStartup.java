package com.icthh.xm.commons.domainevent.db.listener;

import com.icthh.xm.commons.domainevent.db.service.kafka.SystemQueueConsumer;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component("dbDomainEventAppStartApp")
@ConditionalOnProperty(value = "application.auto-system-queue-enabled", havingValue = "true")
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${application.kafka-system-queue}")
    private String kafkaSystemQueue;

    @Value("${application.kafka-metadata-max-age}")
    private Integer kafkaMetadataMaxAge;

    private final SystemQueueConsumer systemQueueConsumer;
    private final KafkaProperties kafkaProperties;
    private final PrivilegeInspector privilegeInspector;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createSystemConsumer(kafkaSystemQueue, systemQueueConsumer::consumeEvent);
        privilegeInspector.readPrivileges(MdcUtils.getRid());
    }

    private void createSystemConsumer(String name, MessageListener<String, String> consumeEvent) {
        log.info("Creating kafka consumer for topic {}", name);
        ContainerProperties containerProps = new ContainerProperties(name);
        containerProps.setObservationEnabled(true);

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, kafkaMetadataMaxAge);
        ConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);

        ConcurrentMessageListenerContainer<String, String> container =
            new ConcurrentMessageListenerContainer<>(factory, containerProps);
        container.setupMessageListener(consumeEvent);
        container.start();
        log.info("Successfully created kafka consumer for topic {}", name);
    }

}
