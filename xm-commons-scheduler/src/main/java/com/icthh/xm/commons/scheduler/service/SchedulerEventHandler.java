package com.icthh.xm.commons.scheduler.service;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;

public interface SchedulerEventHandler {

    String ALL = "ALL";

    void onEvent(ScheduledEvent scheduledEvent);
    default String eventType() {
        return ALL;
    }

}
