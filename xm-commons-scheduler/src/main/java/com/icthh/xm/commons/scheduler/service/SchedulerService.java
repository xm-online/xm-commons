package com.icthh.xm.commons.scheduler.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.resolver.SchedulerEventTypeKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@LepService(group = "scheduler")
public class SchedulerService {

    @LogicExtensionPoint(value = "SchedulerEvent", resolver = SchedulerEventTypeKeyResolver.class)
    public void onEvent(ScheduledEvent scheduledEvent) {
        log.error("No handlers for event {} found", scheduledEvent);
    }
}
