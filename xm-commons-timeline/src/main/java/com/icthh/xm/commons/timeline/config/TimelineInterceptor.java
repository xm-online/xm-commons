package com.icthh.xm.commons.timeline.config;

import com.icthh.xm.commons.timeline.TimelineEventProducer;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.timeline.util.HttpUtils.getResponseContent;
import static com.icthh.xm.commons.timeline.util.JsonUtils.findField;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
@Component
public class TimelineInterceptor implements HandlerInterceptor {

    private static final String HEADER_TENANT = "x-tenant";
    private static final String AUTH_TENANT_KEY = "tenant";
    private static final String AUTH_USER_KEY = "user_key";
    private static final String TYPE_KEY = "typeKey";
    private static final List<String> PREFIXES = asList("$.", "$.xmEntity.");

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

        var auth = getAuthentication();
        if (auth == null) {
            String tenant = request.getHeader(HEADER_TENANT);
            produceTimeline(request, response, tenant, null, null);
        } else {
            Map<String, Object> details = getUserDetails(auth);
            String tenant = String.valueOf(details.getOrDefault(AUTH_TENANT_KEY, ""));
            String userKey = String.valueOf(details.getOrDefault(AUTH_USER_KEY, ""));
            String userLogin = auth.getPrincipal();
            // produce timeline event if enabled
            produceTimeline(request, response, tenant, userLogin, userKey);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserDetails(XmAuthentication auth) {
        Map<String, Object> details = null;
        if (auth.getDetails() != null) {
            details = auth.getDetails().getDecodedDetails();
        }
        details = firstNonNull(details, new HashMap<>());
        return details;
    }

    private static XmAuthentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof XmAuthentication xmAuthentication) {
            return xmAuthentication;
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
        Object typeKey = findField(responseBody, TYPE_KEY, PREFIXES);

        return ignoredTypeKeys.contains(typeKey);
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
