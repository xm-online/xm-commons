package com.icthh.xm.commons.domainevent.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.HttpDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.utils.JsonUtil;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class WebApiSource extends HandlerInterceptorAdapter {

    private static final String HEADER_TENANT = "x-tenant";
    private static final Set<String> EXCLUDE_HEADERS = Set.of("cookie", "authorization");

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final EventPublisher eventPublisher;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final JsonFactory jfactory;

    @Value("${spring.application.name}")
    private String appName;
    private List<ApiMaskRule> maskRules;

    public WebApiSource(EventPublisher eventPublisher, XmAuthenticationContextHolder xmAuthenticationContextHolder,
                        ApiMaskConfig apiIgnore, XmDomainEventConfiguration xmDomainEventConfiguration) {
        this.eventPublisher = eventPublisher;
        this.xmAuthenticationContextHolder = xmAuthenticationContextHolder;
        this.xmDomainEventConfiguration = xmDomainEventConfiguration;
        this.jfactory = new JsonFactory();
        this.maskRules = apiIgnore != null ? apiIgnore.getMaskRules() : null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        if (isIgnoredRequest(request, response)) {
            return;
        }

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

    // TODO
    private boolean isIgnoredRequest(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

    private void publishEvent(HttpServletRequest request, HttpServletResponse response, String tenant, String clientId, String userKey) {
        DomainEvent domainEvent = createEvent(request, response, tenant, clientId, userKey);
        eventPublisher.publish(DefaultDomainEventSource.WEB.getCode(), domainEvent);
    }

    protected DomainEvent createEvent(HttpServletRequest request, HttpServletResponse response, String tenant, String clientId, String userKey) {
        String requestBody = getRequestContent(request);
        String responseBody = getResponseContent(response);

        Pair<String, String> idTypeKey = JsonUtil.extractIdAndTypeKey(jfactory, responseBody);
        return DomainEvent.builder()
            .id(UUID.randomUUID())
            .txId(MdcUtils.getRid())
            .aggregateId(idTypeKey.getKey())
            .aggregateType(idTypeKey.getValue())
            .operation(xmDomainEventConfiguration.getOperationMapping(tenant, request.getMethod(), request.getRequestURI()))
            .msName(appName)
            .source(DefaultDomainEventSource.WEB.getCode())
            .userKey(userKey)
            .clientId(clientId)
            .tenant(tenant)
            .payload(createPayload(request, response, requestBody, responseBody))
            .build();
    }

    private HttpDomainEventPayload createPayload(HttpServletRequest request, HttpServletResponse response,
                                                 String requestBody, String responseBody) {

        HttpDomainEventPayload payload = new HttpDomainEventPayload();
        payload.setMethod(request.getMethod());
        payload.setUrl(request.getRequestURI());
        payload.setQueryString(request.getQueryString());
        payload.setRequestLength(request.getContentLengthLong());
        payload.setRequestBody(maskContent(requestBody, request.getRequestURI(), true, request.getMethod()));
        payload.setResponseBody(maskContent(responseBody, request.getRequestURI(), false, request.getMethod()));
        payload.setResponseLength((long) responseBody.length());
        payload.setRequestHeaders(getRequestHeaders(request));
        payload.setResponseHeaders(getResponseHeaders(response));
        payload.setResponseCode(response.getStatus());
        payload.setExecTime(MdcUtils.getExecTimeMs());
        return payload;
    }

    private HttpHeaders getRequestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!EXCLUDE_HEADERS.contains(name.toLowerCase())) {
                headers.put(name.toLowerCase(), Collections.list(request.getHeaders(name)));
            }
        }
        return HttpHeaders.of(headers, (s1, s2) -> true);
    }

    private HttpHeaders getResponseHeaders(HttpServletResponse httpServletResponse) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String header : httpServletResponse.getHeaderNames()) {
            Collection<String> value = httpServletResponse.getHeaders(header);
            headers.put(header.toLowerCase(), new ArrayList<>(value));
        }
        headers.remove("set-cookie");

        return HttpHeaders.of(headers, (s1, s2) -> true);
    }

    private String maskContent(final String content, String uri, boolean request, String httpMethod) {
        if (CollectionUtils.isEmpty(maskRules) || StringUtils.isBlank(content)) {
            return content;
        }
        return maskRules
            .stream()
            .filter(rule -> ((request && rule.isMaskRequest()) || (!request && rule.isMaskResponse()))
                && matcher.match(rule.getEndpointToMask(), uri)
                && rule.getHttpMethod().stream().anyMatch(method -> StringUtils.equalsIgnoreCase(method, httpMethod)))
            .filter(it -> CollectionUtils.isNotEmpty(it.getPathToMask()))
            .map(rule -> applyMask(content, rule))
            .findAny()
            .orElse(content);
    }

    private String applyMask(final String content, ApiMaskRule rule) {
        DocumentContext documentContext = JsonPath.parse(content);
        boolean isChanged = false;
        for (String path : rule.getPathToMask()) {
            try {
                documentContext.set(path, rule.getMask());
                isChanged = true;
            } catch (PathNotFoundException e) {
                log.warn("Path {} not found, when masking content data", path);
            } catch (Exception e) {
                log.error("Failed to mask content data", e);
            }
        }

        if (!isChanged) {
            return content;
        }

        return documentContext.jsonString();
    }

    private static String getRequestContent(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return new String(((ContentCachingRequestWrapper) request).getContentAsByteArray());
        }
        if (request instanceof HttpServletRequestWrapper
            && ((HttpServletRequestWrapper) request).getRequest() instanceof ContentCachingRequestWrapper) {
            return new String(((ContentCachingRequestWrapper) ((HttpServletRequestWrapper) request)
                .getRequest()).getContentAsByteArray());
        }
        log.warn("Empty request content because of unsupported request class {}", request.getClass());
        return "";
    }

    private static String getResponseContent(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return new String(((ContentCachingResponseWrapper) response).getContentAsByteArray());
        }
        if (response instanceof HttpServletResponseWrapper
            && ((HttpServletResponseWrapper) response).getResponse() instanceof ContentCachingResponseWrapper) {
            return new String(((ContentCachingResponseWrapper) ((HttpServletResponseWrapper) response)
                .getResponse()).getContentAsByteArray());
        }
        log.warn("Empty response content because of unsupported response class {}", response.getClass());
        return "";
    }

}
