package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.XmRelatedComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * The {@link TenantVerifyInterceptor} class.
 */
@XmRelatedComponent
public class TenantVerifyInterceptor implements HandlerInterceptor { // todo spring 3.2.0 migration

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
