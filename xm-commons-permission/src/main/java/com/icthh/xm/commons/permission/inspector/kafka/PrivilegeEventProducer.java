package com.icthh.xm.commons.permission.inspector.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.commons.messaging.event.system.SystemEventType;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.mapper.PrivilegeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivilegeEventProducer {

    private final KafkaTemplate<String, String> template;

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule());

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-queue}")
    private String topicName;

    /**
     * Build message for kafka's event and send it.
     *
     * @param eventId       the event id
     * @param ymlPrivileges the content
     * @deprecated left only for backward compatibility, use {@link #sendEvent(String, Set)}
     */
    @Deprecated
    public void sendEvent(String eventId, String ymlPrivileges) {
        SystemEvent event = buildSystemEvent(eventId, ymlPrivileges);
        serializeEvent(event).ifPresent(this::send);
    }

    /**
     * Build MS_PRIVILEGES message for system queue event and send it.
     *
     * @param eventId    the event id
     * @param privileges the event data (privileges)
     */
    public void sendEvent(String eventId, Set<Privilege> privileges) {
        // TODO do not use yml in json events...
        String ymlPrivileges = PrivilegeMapper.privilegesToYml(privileges);

        SystemEvent event = buildSystemEvent(eventId, ymlPrivileges);
        serializeEvent(event).ifPresent(this::send);
    }

    private Optional<String> serializeEvent(SystemEvent event) {
        try {
            return Optional.ofNullable(mapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.warn("Error while serializing system event: {}", event, e);
        }

        return Optional.empty();
    }

    private SystemEvent buildSystemEvent(String eventId, String ymlPrivileges) {
        SystemEvent event = new SystemEvent();
        event.setEventType(SystemEventType.MS_PRIVILEGES);
        event.setEventId(eventId);
        event.setMessageSource(appName);
        event.setData(Collections.singletonMap("privileges", ymlPrivileges));

        return event;
    }

    /**
     * Send event to system queue.
     *
     * @param content the event content
     */
    private void send(String content) {
        if (!StringUtils.isBlank(content)) {
            log.info("Sending system queue event to kafka-topic = '{}', data = '{}'", topicName, content);
            template.send(topicName, content);
        }
    }

}
