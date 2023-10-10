package com.icthh.xm.commons.domainevent.service;

import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@LepService(group = "event.publisher")
public class EventPublisher {

    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final TenantContextHolder tenantContextHolder;

    @LoggingAspectConfig(inputExcludeParams = "event")
    @LogicExtensionPoint(value = "Publish")
    public void publish(String source, DomainEvent event) {
        event.setSource(source);
        Transport transportToPublish = xmDomainEventConfiguration.getTransport(tenantContextHolder.getTenantKey(), source);
        transportToPublish.send(event);
    }
}
