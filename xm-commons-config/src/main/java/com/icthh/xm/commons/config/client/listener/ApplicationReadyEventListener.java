package com.icthh.xm.commons.config.client.listener;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.kafka.ConfigTopicConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ConsumerFactory<String, String> consumerFactory;
    private final ConfigTopicConsumer configTopicConsumer;
    private final KafkaProperties kafkaProperties;
    private final XmConfigProperties xmConfigProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createSystemConsumer(xmConfigProperties.getKafkaConfigTopic(), configTopicConsumer::consumeEvent);
    }

    private void createSystemConsumer(String name, MessageListener<String, String> consumeEvent) {
        log.info("Creating kafka consumer for topic {}", name);
        ContainerProperties containerProps = new ContainerProperties(name);

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        ConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);

        ConcurrentMessageListenerContainer<String, String> container =
            new ConcurrentMessageListenerContainer<>(factory, containerProps);
        container.setupMessageListener(consumeEvent);
        container.start();
        log.info("Successfully created kafka consumer for topic {}", name);
    }

}
