package com.icthh.xm.commons.lep.spring.web;

import com.icthh.xm.commons.web.spring.XmWebInterceptorProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LepInterceptorProvider implements XmWebInterceptorProvider {

    private final LepInterceptor lepInterceptor;

    @Override
    public AsyncHandlerInterceptor getInterceptor() {
        return lepInterceptor;
    }
}
