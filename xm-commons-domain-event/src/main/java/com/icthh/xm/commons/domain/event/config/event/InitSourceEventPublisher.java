package com.icthh.xm.commons.domain.event.config.event;

import com.icthh.xm.commons.domain.event.config.SourceConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

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
            .forEach(transport -> {
                InitSourceEvent event = new InitSourceEvent(this, tenantKey, transport);
                applicationEventPublisher.publishEvent(event);
            });
    }
}
