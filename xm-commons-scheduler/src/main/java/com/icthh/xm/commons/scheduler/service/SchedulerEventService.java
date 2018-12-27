package com.icthh.xm.commons.scheduler.service;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerEventService {

    private final List<SchedulerEventHandler> handlers;

    public void processSchedulerEvent(ScheduledEvent event) {
        if (event.getEndDate().isBefore(Instant.now())) {
            log.warn("Event skipped because it expired. Event: {}", event);
        }

        if (handlers == null) {
            log.warn("No handlers found. Event: {} skipper.", event);
            return;
        }

        handlers.stream().filter(this::isHandlersForAll).forEach(handler -> handler.onEvent(event));

    }

    private boolean isHandlersForAll(SchedulerEventHandler handler) {
        return SchedulerEventHandler.ALL.equals(handler.eventType());
    }

}
