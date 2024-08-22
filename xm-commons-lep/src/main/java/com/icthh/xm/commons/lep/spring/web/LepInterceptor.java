package com.icthh.xm.commons.lep.spring.web;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.XmRelatedComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@XmRelatedComponent
@Order(5) // 5 - after TenantInterceptor-s
public class LepInterceptor implements AsyncHandlerInterceptor {

    private final LepManagementService lepManagementService;

    public LepInterceptor(@Lazy LepManagementService lepManagementService) {
        this.lepManagementService = lepManagementService;
    }

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
