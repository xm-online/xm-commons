package com.icthh.xm.commons.domain.event.service;

import com.icthh.xm.commons.domain.event.config.SourceConfig;
import com.icthh.xm.commons.domain.event.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@LepService(group = "event.publisher")
public class EventPublisher {

    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final ApplicationContext context;

    @LogicExtensionPoint(value = "Publish")
    public void publish(String source, DomainEvent event) {
        Transport transportToPublish = getTransportBySource(source);
        transportToPublish.send(event);
    }

    private Transport getTransportBySource(String source) {
        SourceConfig interceptorConfig = xmDomainEventConfiguration.getInterceptorConfig(source);
        return context.getBean(interceptorConfig.getTransport());
    }
}
