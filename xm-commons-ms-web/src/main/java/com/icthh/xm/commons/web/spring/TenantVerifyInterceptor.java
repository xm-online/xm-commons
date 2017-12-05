package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The {@link TenantVerifyInterceptor} class.
 */
public class TenantVerifyInterceptor extends HandlerInterceptorAdapter {

    private static final String JSON_ERROR_TENANT_SUSPENDED = "{\"error\": \"Tenant is suspended\"}";

    private final TenantListRepository tenantListRepository;
    private final TenantContextHolder tenantContextHolder;

    public TenantVerifyInterceptor(
                    TenantListRepository tenantListRepository,
                    TenantContextHolder tenantContextHolder) {
        this.tenantListRepository = Objects.requireNonNull(tenantListRepository,
                        "tenantListRepository can't be null");
        this.tenantContextHolder = Objects.requireNonNull(tenantContextHolder,
                        "tenantContextHolder can't be null");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        if (tenantListRepository.getSuspendedTenants().contains(
                        TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder))) {
            InterceptorUtil.sendResponse(response, JSON_ERROR_TENANT_SUSPENDED);
            return false;
        }
        return true;
    }
}
