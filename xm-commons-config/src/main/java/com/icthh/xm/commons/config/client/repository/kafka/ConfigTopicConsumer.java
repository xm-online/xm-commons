package com.icthh.xm.commons.config.client.repository.kafka;

import com.icthh.xm.commons.tenant.JsonMapperUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService;
import com.icthh.xm.commons.config.domain.ConfigEvent;
import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Slf4j
public class ConfigTopicConsumer {

    private final ObjectMapper mapper = JsonMapperUtils.getJsonMapperWithIgnore();

    private final ConfigService configService;
    private final AppliedConfigService appliedConfigService;

    public ConfigTopicConsumer(ConfigService configService) {
        this(configService, null);
    }

    /**
     * @param appliedConfigService records the event version after the event is applied, may be null
     */
    public ConfigTopicConsumer(ConfigService configService, AppliedConfigService appliedConfigService) {
        this.configService = configService;
        this.appliedConfigService = appliedConfigService;
    }

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
                configService.updateConfigurations(event.getCommit(), event.getPaths());
                if (appliedConfigService != null) {
                    appliedConfigService.recordEventVersion(event.getCommit());
                }
            } catch (JacksonException e) {
                log.error("Config topic message has incorrect format: '{}'", message.value(), e);
            }
        } finally {
            MdcUtils.removeRid();
        }
    }
}
