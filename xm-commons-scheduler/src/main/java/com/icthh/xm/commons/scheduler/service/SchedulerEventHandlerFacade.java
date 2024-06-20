package com.icthh.xm.commons.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;

@Slf4j
@RequiredArgsConstructor
public class SchedulerEventHandlerFacade implements MessageHandler {

    private static final String WRAP_TOKEN = "\"";

    private final SchedulerEventService schedulerEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig) {
        try {
            MdcUtils.putRid(MdcUtils.generateRid() + ":" + tenant);
            StopWatch stopWatch = StopWatch.createStarted();
            String payloadString = unwrap(message);
            log.debug("start processing message for tenant: [{}], raw body in base64 = {}", tenant, payloadString);
            String eventBody = new String(Base64.getDecoder().decode(payloadString), UTF_8);
            if (!eventBody.contains("TEST_2")) {
                return;
            }
            log.info("start processing message for tenant: [{}], body = {}", tenant, eventBody);

            schedulerEventService.processSchedulerEvent(mapToEvent(eventBody), tenant);

            log.info("stop processing message for tenant: [{}], time = {}", tenant, stopWatch.getTime());
        } catch (Exception e) {
            log.error("error processing event for tenant [{}]", tenant, e);
            throw e;
        } finally {
            MdcUtils.clear();
        }
    }

    @SneakyThrows
    private ScheduledEvent mapToEvent(String eventBody) {
        return objectMapper.readValue(eventBody, ScheduledEvent.class);
    }

    public static String unwrap(final String str) {
        if (isEmpty(str) || isEmpty(WRAP_TOKEN)) {
            return str;
        }
        if (startsWith(str, WRAP_TOKEN) && endsWith(str, WRAP_TOKEN)) {
            final int startIndex = str.indexOf(WRAP_TOKEN);
            final int endIndex = str.lastIndexOf(WRAP_TOKEN);
            final int wrapLength = WRAP_TOKEN.length();
            if (startIndex != -1 && endIndex != -1) {
                return str.substring(startIndex + wrapLength, endIndex);
            }
        }
        return str;
    }
}
