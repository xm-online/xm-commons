package com.icthh.xm.commons.domainevent.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@UtilityClass
public class HttpContentUtils {

    public static String getResponseContent(HttpServletResponse response) {
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

    public static String getRequestContent(HttpServletRequest request) {
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

    public static HttpHeaders getRequestHeaders(HttpServletRequest request, Set<String> headerNames) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!headerNames.contains(name.toLowerCase())) {
                headers.put(name.toLowerCase(), Collections.list(request.getHeaders(name)));
            }
        }
        return HttpHeaders.of(headers, (s1, s2) -> true);
    }

    public static HttpHeaders getResponseHeaders(HttpServletResponse httpServletResponse, Set<String> headerNames) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String header : httpServletResponse.getHeaderNames()) {
            if (!headerNames.contains(header.toLowerCase())) {
                Collection<String> value = httpServletResponse.getHeaders(header);
                headers.put(header.toLowerCase(), new ArrayList<>(value));
            }
        }
        headers.remove("set-cookie");

        return HttpHeaders.of(headers, (s1, s2) -> true);
    }
}
