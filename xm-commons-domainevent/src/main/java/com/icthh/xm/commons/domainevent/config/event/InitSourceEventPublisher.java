package com.icthh.xm.commons.domainevent.config.event;

import com.icthh.xm.commons.domainevent.config.SourceConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public
class InitSourceEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(String tenantKey, Collection<SourceConfig> sources) {
        sources
                .stream()
                .filter(SourceConfig::isEnabled)
                .map(SourceConfig::getTransport)
                .distinct()
                .map(transport -> new InitSourceEvent(this, tenantKey, transport))
                .forEach(applicationEventPublisher::publishEvent);
    }
}
