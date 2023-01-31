package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.filter.WebApiDomainEventFactory;
import com.icthh.xm.commons.domainevent.service.filter.WebFilterEngine;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Slf4j
@Component
public class WebApiSource extends HandlerInterceptorAdapter {

    private static final String HEADER_TENANT = "x-tenant";

    @Value("${spring.application.name}")
    private String appName;

    private final EventPublisher eventPublisher;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final WebApiDomainEventFactory webApiDomainEventFactory;
    private final WebFilterEngine webFilterEngine;

    public WebApiSource(EventPublisher eventPublisher, XmAuthenticationContextHolder xmAuthenticationContextHolder,
                        ApiMaskConfig apiIgnore, XmDomainEventConfiguration xmDomainEventConfiguration, WebFilterEngine webFilterEngine) {
        this.eventPublisher = eventPublisher;
        this.xmAuthenticationContextHolder = xmAuthenticationContextHolder;

        List<ApiMaskRule> maskRules = apiIgnore != null ? apiIgnore.getMaskRules() : null;
        this.webFilterEngine = webFilterEngine;
        this.webApiDomainEventFactory = new WebApiDomainEventFactory(appName, xmDomainEventConfiguration, maskRules);
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
                webApiDomainEventFactory.createEvent(request, response, tenant, clientId, userKey, aggregateDetails, responseBody));

        if (domainEvent == null) {
            return;
        }

        eventPublisher.publish(DefaultDomainEventSource.WEB.getCode(), domainEvent);
    }
}
