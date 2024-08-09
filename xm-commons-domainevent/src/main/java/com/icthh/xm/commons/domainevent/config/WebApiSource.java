package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.filter.DomainEventProvider;
import com.icthh.xm.commons.domainevent.service.filter.DomainEventProviderFactory;
import com.icthh.xm.commons.domainevent.service.filter.WebFilterEngine;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Slf4j
@Component
@ConditionalOnProperty(value = "application.domain-event.enabled", havingValue = "true")
public class WebApiSource implements AsyncHandlerInterceptor {

    private static final String HEADER_TENANT = "x-tenant";

    private final EventPublisher eventPublisher;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final DomainEventProvider domainEventProvider;
    private final WebFilterEngine webFilterEngine;

    public WebApiSource(EventPublisher eventPublisher, XmAuthenticationContextHolder xmAuthenticationContextHolder,
                        WebFilterEngine webFilterEngine, DomainEventProviderFactory domainEventProviderFactory) {
        this.eventPublisher = eventPublisher;
        this.xmAuthenticationContextHolder = xmAuthenticationContextHolder;
        this.webFilterEngine = webFilterEngine;
        this.domainEventProvider = domainEventProviderFactory.newDomainEventProvider();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        XmAuthenticationContext auth = xmAuthenticationContextHolder.getContext();
        if (auth == null || auth.isAnonymous()) {
            String tenant = request.getHeader(HEADER_TENANT);
            publishEvent(request, response, tenant, null, null);
        } else {
            String clientId = auth.getClientId().orElse("");
            String userKey = auth.getUserKey().orElse("");
            String tenant = auth.getTenantName().orElse("");

            publishEvent(request, response, tenant, clientId, userKey);
        }
    }

    private void publishEvent(HttpServletRequest request, HttpServletResponse response, String tenant, String clientId, String userKey) {
        DomainEvent domainEvent = webFilterEngine.isIgnoreRequest(request, response, tenant,
            (String[] aggregateDetails, String responseBody) ->
                domainEventProvider.createEvent(request, response, tenant, clientId, userKey, aggregateDetails, responseBody));

        if (domainEvent == null) {
            return;
        }

        eventPublisher.publish(DefaultDomainEventSource.WEB.getCode(), domainEvent);
    }
}
