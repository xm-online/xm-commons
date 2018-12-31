package com.icthh.xm.commons.scheduler.service;

import static java.util.stream.Collectors.toList;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerEventService {

    private final List<SchedulerEventHandler> handlers;

    public void processSchedulerEvent(ScheduledEvent event, String tenantKey) {
        if (event.getEndDate() != null && event.getEndDate().isBefore(Instant.now())) {
            log.warn("Event skipped because it expired. Event: {}", event);
            return;
        }

        if (handlers == null) {
            log.warn("No handlers found. Event: {} skipper.", event);
            return;
        }

        List<SchedulerEventHandler> eventHandlers = handlers.stream()
            .filter(handler -> Objects.equals(handler.eventType(), event.getTypeKey()) || isHandlersForAll(handler))
            .collect(toList());
        if (eventHandlers.isEmpty()) {
            log.warn("No handlers found. Event: {} skipper.", event);
        } else {
            log.warn("Fount {} handlers found. Event: {} skipper.", eventHandlers.size(), event);
        }

        try {
            eventHandlers.forEach(handler -> handler.onEvent(event, tenantKey));
        } catch (Exception e) {
            log.error("Error process message {}", event);
            if (event.getEndDate() != null) {
                throw e;
            } else {
                log.info("End date in null. Error will be skiped");
            }
        }
    }

    private boolean isHandlersForAll(SchedulerEventHandler handler) {
        return SchedulerEventHandler.ALL.equals(handler.eventType());
    }

}
