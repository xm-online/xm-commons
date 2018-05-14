package com.icthh.xm.commons.config.client.repository.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.repository.ConfigurationModel;
import com.icthh.xm.commons.config.domain.ConfigEvent;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.config.domain.ConfigurationEvent;
import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ConfigTopicConsumer {

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());

    private final ConfigurationModel configurationModel;

    /**
     * Consume tenant command event message.
     *
     * @param message the tenant command event message
     */
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MdcUtils.putRid();
        try {
            log.info("Consume event from topic [{}]", message.topic());
            try {
                ConfigEvent event = mapper.readValue(message.value(), ConfigEvent.class);

                log.info("Process event from topic [{}], source='{}', event_id ='{}'",
                    message.topic(), event.getMessageSource(), event.getEventId());
                onSaveConfiguration(event);
            } catch (IOException e) {
                log.error("System queue message has incorrect format: '{}'", message.value(), e);
            }
        } finally {
            MdcUtils.removeRid();
        }
    }

    private void onSaveConfiguration(ConfigEvent event) {
        List<ConfigurationEvent> configurations = event.getConfigurations();
        configurationModel.updateConfiguration(configurations.stream().map(
            configurationEvent -> new Configuration(configurationEvent.getPath(), null,
                configurationEvent.getCommit())).collect(Collectors
            .toList()));
    }
}
