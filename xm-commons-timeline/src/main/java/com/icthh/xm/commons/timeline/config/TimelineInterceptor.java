package com.icthh.xm.commons.timeline.config;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.icthh.xm.commons.timeline.TimelineEventProducer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
public class TimelineInterceptor extends HandlerInterceptorAdapter {

    private static final String HEADER_TENANT = "x-tenant";
    private static final String AUTH_TENANT_KEY = "tenant";
    private static final String AUTH_USER_KEY = "user_key";
    private static final String TYPE_KEY = "typeKey";

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final TimelineEventProducer eventProducer;
    private final List<String> ignoredPatterns;
    private final List<String> ignoredHttpMethods;
    private final List<String> ignoredTypeKeys;

    public TimelineInterceptor(
        TimelineEventProducer eventProducer,
        @Value("${application.tenant-ignored-path-list:true}") List<String> ignoredPatterns,
        @Value("${application.timeline-ignored-http-methods:#{T(java.util.Collections).emptyList()}}") List<String> ignoredHttpMethods,
        @Value("${application.timeline-ignored-type-keys:#{T(java.util.Collections).emptyList()}}") List<String> ignoredTypeKeys
        ) {
        this.eventProducer = eventProducer;
        this.ignoredPatterns = ignoredPatterns;
        this.ignoredHttpMethods = ignoredHttpMethods;
        this.ignoredTypeKeys = ignoredTypeKeys;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        if (isIgnoredRequest(request, response)) {
            return;
        }

        final OAuth2Authentication auth = getAuthentication();
        if (auth == null) {
            String tenant = request.getHeader(HEADER_TENANT);
            produceTimeline(request, response, tenant, null, null);
        } else {
            Map<String, String> details = getUserDetails(auth);
            String tenant = details.getOrDefault(AUTH_TENANT_KEY, "");
            String userKey = details.getOrDefault(AUTH_USER_KEY, "");
            String userLogin = (String) auth.getPrincipal();
            // produce timeline event if enabled
            produceTimeline(request, response, tenant, userLogin, userKey);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getUserDetails(OAuth2Authentication auth) {
        Map<String, String> details = null;
        if (auth.getDetails() != null) {
            details = Map.class.cast(OAuth2AuthenticationDetails.class.cast(auth.getDetails()).getDecodedDetails());
        }
        details = firstNonNull(details, new HashMap<>());
        return details;
    }

    private static OAuth2Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2Authentication) {
            return (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        }
        return null;
    }

    private boolean isIgnoredRequest(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getServletPath();
        String httpMethod = request.getMethod();

        if (isNotEmpty(ignoredHttpMethods) && ignoredHttpMethods.contains(httpMethod)) {
            return true;
        }

        if (ignoredPatterns != null && path != null) {
            for (String pattern : ignoredPatterns) {
                if (matcher.match(pattern, path)) {
                    return true;
                }
            }
        }

        if (isEmpty(ignoredTypeKeys)) {
            return false;
        }

        String responseBody = getResponseContent(response);
        String typeKey = getEntityField(responseBody, TYPE_KEY);

        return ignoredTypeKeys.contains(typeKey);
    }

    private String getResponseContent(HttpServletResponse response) {
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

    public String getEntityField(String entity, String field) {
        List<String> prefixes = asList("$.", "$.xmEntity.");

        for (String prefix: prefixes) {
            try {
                return JsonPath.read(entity, prefix + field);
            } catch (Exception ex) {
                log.trace("JsonPath exception", ex);
            }
        }

        return "";
    }

    private void produceTimeline(
        HttpServletRequest request,
        HttpServletResponse response,
        String tenant,
        String userLogin,
        String userKey
    ) {
        String content = eventProducer.createEventJson(request, response, tenant, userLogin, userKey);
        eventProducer.send(tenant, content);
    }

}
