package com.icthh.xm.commons.domainevent.db.service.kafka;


import com.icthh.xm.commons.domainevent.db.lep.SystemQueueConsumerLepKeyResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@LepService(group = "queue.system")
public class SystemConsumerService {

    @LogicExtensionPoint(value = "AcceptSystemEvent", resolver = SystemQueueConsumerLepKeyResolver.class)
    public void acceptSystemEvent(SystemEvent event) {
        log.warn("System event type {} not supported.", event.getEventType());
    }
}
