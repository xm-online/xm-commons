package com.icthh.xm.commons.timeline.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@Slf4j
@UtilityClass
public class HttpUtils {

    public static String getRequestContent(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper contentCachingRequestWrapper) {
            return new String(contentCachingRequestWrapper.getContentAsByteArray());
        }
        if (request instanceof HttpServletRequestWrapper httpServletRequestWrapper
            && httpServletRequestWrapper.getRequest() instanceof ContentCachingRequestWrapper contentCachingRequestWrapper) {
            return new String(contentCachingRequestWrapper.getContentAsByteArray());
        }
        log.warn("Empty request content because of unsupported request class {}", request.getClass());
        return "";
    }

    public static String getResponseContent(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper cachingResponseWrapper) {
            return new String(cachingResponseWrapper.getContentAsByteArray());
        }
        if (response instanceof HttpServletResponseWrapper httpServletResponseWrapper
            && httpServletResponseWrapper.getResponse() instanceof ContentCachingResponseWrapper contentCachingResponseWrapper) {
            return new String(contentCachingResponseWrapper.getContentAsByteArray());
        }
        log.warn("Empty response content because of unsupported response class {}", response.getClass());
        return "";
    }

}
