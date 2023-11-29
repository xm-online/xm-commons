package com.icthh.xm.commons.web.spring;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@RequiredArgsConstructor
public class TenantVerifyInterceptorProvider implements XmWebInterceptorProvider {

    private final TenantVerifyInterceptor tenantVerifyInterceptor;

    @Override
    public AsyncHandlerInterceptor getInterceptor() {
        return tenantVerifyInterceptor;
    }
}
