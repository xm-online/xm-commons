package com.icthh.xm.commons.config.client.repository.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.ConfigEvent;
import com.icthh.xm.commons.logging.util.MdcUtils;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class ConfigTopicConsumer {

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());

    private final ConfigService configService;
    private final List<RefreshableConfiguration> refreshableConfigurations;

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

                log.info("Process event from topic [{}], event_id ='{}', commit: '{}'",
                    message.topic(), event.getEventId(), event.getCommit());
                Set<String> paths = event.getPaths();
                log.debug("consumeEvent: total {} paths from event: {}", paths.size(), paths);

                Set<String> filteredPaths = filterListeningPaths(paths);
                log.debug("consumeEvent: filtered {} paths from event: {}", filteredPaths.size(), filteredPaths);

                log.info("consumeEvent: filtered {} paths from {} total paths", filteredPaths.size(), paths.size());
                if (filteredPaths.isEmpty()) {
                    log.info("No listening configuration paths found in event paths, skip update");
                } else {
                    configService.updateConfigurations(event.getCommit(), filteredPaths);
                }
            } catch (IOException e) {
                log.error("Config topic message has incorrect format: '{}'", message.value(), e);
            }
        } finally {
            MdcUtils.removeRid();
        }
    }

    private Set<String> filterListeningPaths(Set<String> paths) {
        return paths.stream()
            .filter(this::isAnyConfigurationListening)
            .collect(Collectors.toSet());
    }

    private boolean isAnyConfigurationListening(String path) {
        return refreshableConfigurations.stream()
            .anyMatch(config -> config.isListeningConfiguration(path));
    }
}
