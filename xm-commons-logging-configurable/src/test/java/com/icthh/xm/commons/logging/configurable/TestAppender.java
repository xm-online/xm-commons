package com.icthh.xm.commons.logging.configurable;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestAppender extends AppenderBase<ILoggingEvent> {

    private static List<ILoggingEvent> events = new ArrayList<>();

    @Override
    public void append(ILoggingEvent e) {
        events.add(e);
    }

    public static List<ILoggingEvent> getEvents() {
        return events;
    }

    public static void clearEvents() {
        events.clear();
    }

    public static ILoggingEvent searchByMessage(String message) {
        return findMessage(message)
                     .orElseThrow(() -> new RuntimeException("Can not find message: " + message));
    }

    public static Optional<ILoggingEvent> findMessage(String message) {
        return events.stream()
            .filter(event -> event.getMessage().equals(message))
            .findFirst();
    }
}

