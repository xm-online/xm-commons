package com.icthh.xm.commons.lep.spring.web;

import com.icthh.xm.commons.lep.api.LepManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class LepInterceptor implements AsyncHandlerInterceptor {

    private final LepManagementService lepManagementService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        lepManagementService.beginThreadContext();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        lepManagementService.endThreadContext();
    }

}
