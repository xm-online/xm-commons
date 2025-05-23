package com.icthh.xm.commons.topic.config;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.logging.trace.TraceWrapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfigurationService;
import com.icthh.xm.commons.topic.service.TopicConfigurationService;
import com.icthh.xm.commons.topic.service.TopicDynamicConsumerConfiguration;
import com.icthh.xm.commons.topic.service.TopicManagerService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Slf4j
@TestConfiguration
@RequiredArgsConstructor
public class TestBeanConfiguration {

    private static final String APP_NAME = "some-ms";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    @Bean
    public TopicDynamicConsumerConfiguration topicDynamicConsumerConfiguration() {
        return new TopicDynamicConsumerConfiguration(applicationEventPublisher, messageHandler());
    }

    @Bean
    public TopicConfigurationService topicConfigurationService() {
        return new TopicConfigurationService(APP_NAME, topicDynamicConsumerConfiguration());
    }

    @Bean
    public DynamicConsumerConfigurationService dynamicConsumerConfigurationService() {
        return new DynamicConsumerConfigurationService(
            List.of(topicDynamicConsumerConfiguration()),
            topicManagerService(),
            mock(TenantListRepository.class)
        );
    }

    @Bean
    public TopicManagerService topicManagerService() {
        return new TopicManagerService(kafkaProperties, kafkaTemplate, traceWrapper());
    }

    @Bean
    public TraceWrapper traceWrapper() {
        return mock(TraceWrapper.class);
    }

    @Bean
    public MessageHandler messageHandler() {
        return spy(new MessageHandler() { // don't use lamda!
            @Override
            public void onMessage(String message, String tenant, TopicConfig topicConfig, Map<String, byte[]> headers) {
                log.info("Handle message = {}", message);
            }
        });
    }
}
