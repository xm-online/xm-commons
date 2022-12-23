package com.icthh.xm.commons.timeline.config;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.HttpDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventTimelineInterceptor extends HandlerInterceptorAdapter {

    private static final String HEADER_TENANT = "x-tenant";
    private static final String AUTH_TENANT_KEY = "tenant";
    private static final String AUTH_USER_KEY = "user_key";

    private final EventPublisher eventPublisher;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        if (isIgnoredRequest(request, response)) {
            return;
        }

        final OAuth2Authentication auth = getAuthentication();
        if (auth == null) {
            String tenant = request.getHeader(HEADER_TENANT);
            publishEvent(request, response, tenant, null, null);
        } else {
            Map<String, String> details = getUserDetails(auth);
            String tenant = details.getOrDefault(AUTH_TENANT_KEY, "");
            String userKey = details.getOrDefault(AUTH_USER_KEY, "");
            String userLogin = (String) auth.getPrincipal();
            publishEvent(request, response, tenant, userLogin, userKey);
        }
    }

    private OAuth2Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2Authentication) {
            return (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        }
        return null;
    }

    // TODO
    private boolean isIgnoredRequest(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

    private Map<String, String> getUserDetails(OAuth2Authentication auth) {
        Map<String, String> details = null;
        if (auth.getDetails() != null) {
            details = Map.class.cast(OAuth2AuthenticationDetails.class.cast(auth.getDetails()).getDecodedDetails());
        }
        details = firstNonNull(details, new HashMap<>());
        return details;
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
