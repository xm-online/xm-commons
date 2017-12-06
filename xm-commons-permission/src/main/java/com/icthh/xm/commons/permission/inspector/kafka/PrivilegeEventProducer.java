package com.icthh.xm.commons.permission.inspector.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivilegeEventProducer {

    private final KafkaTemplate<String, String> template;
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(new JavaTimeModule());

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-queue}")
    private String topicName;

    /**
     * Build message for kafka's event and send it.
     *
     * @param eventId the event id
     * @param yml     the content
     */
    public void sendEvent(String eventId, String yml) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", eventId);
            map.put("messageSource", appName);
            map.put("eventType", "MS_PRIVILEGES");
            map.put("startDate", Instant.now().toString());
            map.put("data", Collections.singletonMap("privileges", yml));
            send(mapper.writeValueAsString(map));
        } catch (JsonProcessingException e) {
            log.warn("Error creating MS_PRIVILEGES event", e);
        }
    }

    /**
     * Send event to kafka.
     *
     * @param content the event content
     */
    private void send(String content) {
        if (!StringUtils.isBlank(content)) {
            log.info("Sending kafka event to topic = '{}', data = '{}'", topicName, content);
            template.send(topicName, content);
        }
    }
}
