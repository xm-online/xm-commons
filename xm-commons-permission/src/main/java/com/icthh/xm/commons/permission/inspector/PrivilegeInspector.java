package com.icthh.xm.commons.permission.inspector;

import com.icthh.xm.commons.permission.inspector.kafka.PrivilegeEventProducer;
import com.icthh.xm.commons.permission.inspector.scanner.PrivilegeScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrivilegeInspector {

    private final PrivilegeScanner scanner;
    private final PrivilegeEventProducer eventProducer;

    /**
     * Scan for permission annotations and send event to kafka.
     */
    @Async
    public void readPrivileges(String eventId) {
        eventProducer.sendEvent(eventId, scanner.scan());
    }
}
