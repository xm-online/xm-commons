package com.icthh.xm.commons.timeline;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.timeline.domain.ApiMaskConfig;
import com.icthh.xm.commons.timeline.domain.ApiMaskRule;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@Slf4j
@Component
public class TimelineEventProducer {

    private final KafkaTemplate<Integer, String> template;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Value("${spring.application.name}")
    private String appName;

    private List<ApiMaskRule> maskRules;

    public TimelineEventProducer(KafkaTemplate<Integer, String> template,
                                 ApiMaskConfig apiIgnore) {
        this.template = template;
        this.maskRules = apiIgnore != null ? apiIgnore.getMaskRules() : null;
    }

    /**
     * Create event json string.
     *
     * @param request  the http request
     * @param response the http response
     */
    public String createEventJson(HttpServletRequest request,
                                  HttpServletResponse response,
                                  String tenant,
                                  String userLogin,
                                  String userKey) {
        try {
            String requestBody = getRequestContent(request);
            String responseBody = getResponseContent(response);

            Instant startDate = Instant.ofEpochMilli(System.currentTimeMillis() - MdcUtils.getExecTimeMs());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rid", MdcUtils.getRid());
            data.put("login", userLogin);
            data.put("userKey", userKey);
            data.put("tenant", tenant);
            data.put("msName", appName);
            data.put("operationName", getResourceName(request.getRequestURI())
                + " " + getOperation(request.getMethod()));
            data.put("operationUrl", request.getRequestURI());
            data.put("operationQueryString", request.getQueryString());
            data.put("startDate", startDate.toString());
            data.put("httpMethod", request.getMethod());
            data.put("requestBody", maskContent(requestBody, request.getRequestURI(), true, request.getMethod()));
            data.put("requestLength", requestBody.length());
            data.put("responseBody", maskContent(responseBody, request.getRequestURI(), false, request.getMethod()));
            data.put("responseLength", responseBody.length());
            data.put("requestHeaders", getRequestHeaders(request));
            data.put("responseHeaders", getResponseHeaders(response));
            data.put("httpStatusCode", response.getStatus());
            data.put("channelType", "HTTP");
            data.put("entityId", getEntityField(responseBody, "id"));
            data.put("entityKey", getEntityField(responseBody, "key"));
            data.put("entityTypeKey", getEntityField(responseBody, "typeKey"));
            data.put("execTime", MdcUtils.getExecTimeMs());

            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Error creating timeline event", e);
        }
        return null;
    }

    /**
     * Send event to kafka.
     *
     * @param topic   the kafka topic
     * @param content the event content
     */
    @Async
    public void send(String topic, String content) {
        try {
            if (!StringUtils.isBlank(content)) {
                log.debug("Sending kafka event with data {} to topic {}", content, topic);
                template.send(topic, content);
            }
        } catch (Exception e) {
            log.error("Error send timeline event", e);
            throw e;
        }
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

    private static Map<String, Object> getRequestHeaders(HttpServletRequest request) {
        Map<String, Object> headers = new LinkedHashMap<>();
        Set<String> excludedHeaders = getExcludeHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!excludedHeaders.contains(name.toLowerCase())) {
                headers.put(name.toLowerCase(), getHeaderValue(request, name));
            }
        }
        return headers;
    }

    private static Set<String> getExcludeHeaders() {
        Set<String> excludedHeaders = new HashSet<>();
        excludedHeaders.add("cookie");
        excludedHeaders.add("authorization");
        return excludedHeaders;
    }

    private static Object getHeaderValue(HttpServletRequest request, String name) {
        List<String> value = Collections.list(request.getHeaders(name));
        if (value.size() == 1) {
            return value.get(0);
        }
        if (value.isEmpty()) {
            return "";
        }
        return value;
    }

    private static Map<String, String> getResponseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String header : response.getHeaderNames()) {
            String value = response.getHeader(header);
            headers.put(header.toLowerCase(), value);
        }
        headers.remove("set-cookie");
        return headers;
    }

    private static Object getEntityField(String entity, String field) {
        List<String> prefixes = asList("$.", "$.xmEntity.", "$.data.");

        for (String prefix: prefixes) {
            try {
                return JsonPath.read(entity, prefix + field);
            } catch (Exception ex) {
                log.trace("JsonPath exception", ex);
            }
        }

        return "";
    }

    private static String getOperation(String method) {
        switch (method) {
            case "GET":
                return "viewed";
            case "POST":
                return "created";
            case "PUT":
                return "changed";
            case "DELETE":
                return "deleted";
            default:
                return "";
        }
    }

    private static String getResourceName(String path) {
        String name = StringUtils.removeStart(path, "/api/");
        if (StringUtils.startsWith(name, "_search")) {
            name = StringUtils.substringAfter(name, "/");
        }
        return StringUtils.defaultIfBlank(StringUtils.substringBefore(name, "/"), "unknown");
    }

    private String maskContent(String content, String uri, boolean request, String httpMethod) {
        if (CollectionUtils.isEmpty(maskRules) || StringUtils.isBlank(content)) {
            return content;
        }
        return maskRules
            .stream()
            .filter(rule -> ((request && rule.isMaskRequest()) || (!request && rule.isMaskResponse()))
                && matcher.match(rule.getEndpointToMask(), uri)
                && rule.getHttpMethod().stream().anyMatch(method -> StringUtils.equalsIgnoreCase(method, httpMethod)))
            .map(rule -> applyMask(content, rule))
            .findAny()
            .orElse(content);
    }

    private static String applyMask(String content, ApiMaskRule rule) {
        String maskedContent = content;
        for (String path : rule.getPathToMask()) {
            try {
                maskedContent = JsonPath.parse(maskedContent).set(path, rule.getMask()).jsonString();
            } catch (PathNotFoundException e) {
                log.debug("Path {} not found, when masking content data", path);
            } catch (Exception e) {
                log.warn("Failed to mask content data", e);
            }
        }
        return maskedContent;
    }
}
