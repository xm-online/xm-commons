package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.HttpDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebApiSource extends HandlerInterceptorAdapter {

    private static final String HEADER_TENANT = "x-tenant";

    private final EventPublisher eventPublisher;
    private final SpringSecurityXmAuthenticationContextHolder springSecurityXmAuthenticationContextHolder;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        if (isIgnoredRequest(request, response)) {
            return;
        }

        XmAuthenticationContext auth = springSecurityXmAuthenticationContextHolder.getContext();
        if (auth == null || auth.isAnonymous()) {
            String tenant = request.getHeader(HEADER_TENANT);
            publishEvent(request, response, tenant, null, null);
        } else {
            String userLogin = auth.getLogin().orElse("");
            String userKey = auth.getUserKey().orElse("");
            String tenant = auth.getTenantName().orElse("");

            publishEvent(request, response, tenant, userLogin, userKey);
        }
    }

    // TODO
    private boolean isIgnoredRequest(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

    private void publishEvent(HttpServletRequest request, HttpServletResponse response, String tenant, String userLogin, String userKey) {
        DomainEvent domainEvent = DomainEvent.builder()
            .id(UUID.randomUUID())
            .tenant(tenant)
            .userKey(userKey)
            .payload(createPayload(request, response))
            .build();
        eventPublisher.publish(DefaultDomainEventSource.HTTP.name(), domainEvent);
    }

    @SneakyThrows
    private HttpDomainEventPayload createPayload(HttpServletRequest request, HttpServletResponse response) {
        ContentCachingResponseWrapper responseCacheWrapperObject = new ContentCachingResponseWrapper(response);

        String requestBody = IOUtils.toString(request.getReader());
        String responseBody = IOUtils.toString(responseCacheWrapperObject.getContentInputStream(), StandardCharsets.UTF_8);

        HttpDomainEventPayload payload = new HttpDomainEventPayload();
        payload.setMethod(request.getMethod());
        payload.setUrl(request.getRequestURI());
        payload.setQueryString(request.getQueryString());
        payload.setRequestLength(request.getContentLengthLong());
        payload.setRequestBody(requestBody);
        payload.setResponseBody(responseBody);
        payload.setResponseLength((long) responseBody.length());
        payload.setRequestHeaders(getRequestHeaders(request));
        payload.setResponseHeaders(getResponseHeaders(response));
        payload.setResponseCode(response.getStatus());
        payload.setExecTime(MdcUtils.getExecTimeMs());
        return payload;
    }

    private HttpHeaders getRequestHeaders(HttpServletRequest httpRequest) {
        Map<String, List<String>> headersMap = Collections.list(httpRequest.getHeaderNames())
            .stream()
            .collect(Collectors.toMap(
                Function.identity(),
                h -> Collections.list(httpRequest.getHeaders(h))
            ));

        return HttpHeaders.of(headersMap, (s1, s2) -> true);
    }

    private HttpHeaders getResponseHeaders(HttpServletResponse httpServletResponse) {
        Map<String, List<String>> headersMap = httpServletResponse.getHeaderNames().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                h -> new ArrayList<>(httpServletResponse.getHeaders(h))
            ));

        return HttpHeaders.of(headersMap, (s1, s2) -> true);
    }

}
