package com.icthh.xm.commons.request.spring;

import com.icthh.xm.commons.request.XmPrivilegedRequestContext;
import com.icthh.xm.commons.request.XmRequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * The {@link XmRequestContextInterceptor} class.
 */
public class XmRequestContextInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmRequestContextInterceptor.class);

    private final XmRequestContextHolder requestContextHolder;
    private final String contextRequestSourceKey;
    private final Object requestSourceType;

    public XmRequestContextInterceptor(XmRequestContextHolder requestContextHolder,
                                       String contextRequestSourceKey,
                                       Object requestSourceType) {
        this.requestContextHolder = requestContextHolder;
        this.contextRequestSourceKey = contextRequestSourceKey;
        this.requestSourceType = requestSourceType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {
        LOGGER.debug("Init XM request context for {} interceptor", requestSourceType);
        getXmPrivilegedRequestContext().putValue(contextRequestSourceKey, requestSourceType);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        LOGGER.debug("Destroy XM request context");
        getXmPrivilegedRequestContext().destroyCurrentContext();
    }

    private XmPrivilegedRequestContext getXmPrivilegedRequestContext() {
        return requestContextHolder.getPrivilegedContext();
    }

}
