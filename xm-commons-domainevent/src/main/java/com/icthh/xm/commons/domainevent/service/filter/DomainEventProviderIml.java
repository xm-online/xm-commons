package com.icthh.xm.commons.domainevent.service.filter;


import com.icthh.xm.commons.domainevent.config.ApiMaskRule;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.HttpDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.utils.HttpContentUtils;
import com.icthh.xm.commons.domainevent.utils.JsonUtil;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DomainEventProviderIml implements DomainEventProvider {

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final String appName;
    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final List<ApiMaskRule> maskRules;

    @Override
    public DomainEvent createEvent(HttpServletRequest request, HttpServletResponse response, String tenant,
                                      String clientId, String userKey, String[] aggregateDetails, String responseBody) {
        String requestBody = HttpContentUtils.getRequestContent(request);

        return DomainEvent.builder()
            .id(UUID.randomUUID())
            .txId(MdcUtils.getRid())
            .aggregateId(JsonUtil.AggregateMapper.getId(aggregateDetails))
            .aggregateName(JsonUtil.AggregateMapper.getName(aggregateDetails))
            .aggregateType(JsonUtil.AggregateMapper.getTypeKey(aggregateDetails))
            .operation(xmDomainEventConfiguration.getOperationMapping(tenant, request.getMethod(), request.getRequestURI()))
            .msName(appName)
            .source(DefaultDomainEventSource.WEB.getCode())
            .userKey(userKey)
            .clientId(clientId)
            .tenant(tenant)
            .payload(createPayload(tenant, request, response, requestBody, responseBody))
            .build();
    }

    private HttpDomainEventPayload createPayload(String tenant, HttpServletRequest request, HttpServletResponse response,
                                                 String requestBody, String responseBody) {

        Set<String> tenantHeaders = xmDomainEventConfiguration.getTenantHeaders(tenant);
        HttpDomainEventPayload payload = new HttpDomainEventPayload();
        payload.setMethod(request.getMethod());
        payload.setUrl(request.getRequestURI());
        payload.setQueryString(request.getQueryString());
        payload.setRequestLength(request.getContentLengthLong());
        payload.setRequestBody(maskContent(requestBody, request.getRequestURI(), true, request.getMethod()));
        payload.setResponseBody(maskContent(responseBody, request.getRequestURI(), false, request.getMethod()));
        payload.setResponseLength((long) responseBody.length());
        payload.setRequestHeaders(HttpContentUtils.getRequestHeaders(request, tenantHeaders));
        payload.setResponseHeaders(HttpContentUtils.getResponseHeaders(response, tenantHeaders));
        payload.setResponseCode(response.getStatus());
        payload.setExecTime(MdcUtils.getExecTimeMs());
        return payload;
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
}
