package com.icthh.xm.commons.web.spring;

import org.springframework.web.servlet.AsyncHandlerInterceptor;

@FunctionalInterface
public interface XmWebInterceptorProvider {
    AsyncHandlerInterceptor getInterceptor();
}
