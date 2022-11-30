package com.icthh.xm.commons.domain.event.service.builder.impl;

import com.icthh.xm.commons.domain.event.service.builder.DomainEventBuilder;
import com.icthh.xm.commons.domain.event.domain.DomainEvent;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component("defaultDomainEventBuilder")
@RequiredArgsConstructor
public class DefaultDomainEventBuilder implements DomainEventBuilder {

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Value("${spring.application.name}")
    private String msName;

    @Override
    public DomainEvent.DomainEventBuilder getPrefilledBuilder() {
        XmAuthenticationContext context = xmAuthenticationContextHolder.getContext();
        return DomainEvent
            .builder()
            .msName(msName)
            .tenant(tenantContextHolder.getTenantKey())
            .eventDate(Instant.now())
            .clientId(context == null ? null : context.getClientId().orElse(null))
            .userKey(context == null ? null : context.getUserKey().orElse(null));
    }
}
