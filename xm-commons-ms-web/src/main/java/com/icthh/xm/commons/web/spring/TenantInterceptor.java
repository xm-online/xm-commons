package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.XmTenantConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The {@link TenantInterceptor} class.
 */
// Add exclusion path to config:
// https://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/mvc.html#mvc-config-interceptors
public class TenantInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantInterceptor.class);

    private static final String JSON_ERROR_NO_TENANT_SUPPLIED = "{\"error\": \"No tenant supplied.\"}";

    private final TenantContextHolder tenantContextHolder;

    private final XmAuthenticationContextHolder xmAuthContextHolder;

    public TenantInterceptor(XmAuthenticationContextHolder xmAuthenticationContextHolder,
                             TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = Objects.requireNonNull(tenantContextHolder,
                                                          "tenantContextHolder can't be null");
        this.xmAuthContextHolder = Objects.requireNonNull(xmAuthenticationContextHolder,
                                                          "xmAuthenticationContextHolder can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {
        final XmAuthenticationContext authContext = xmAuthContextHolder.getContext();

        boolean isFullyAuthenticated = authContext.isFullyAuthenticated();

        // getValue tenant name
        Optional<String> tenantName;
        if (isFullyAuthenticated) {
            // getValue tenant name from authentication token
            tenantName = authContext.getDetailsValue(XmTenantConstants.AUTH_CONTEXT_TENANT_NAME);
        } else {
            // getValue tenant name from request header
            String tenantNameValue = request.getHeader(XmTenantConstants.HTTP_HEADER_TENANT_NAME);
            tenantName = StringUtils.isNotBlank(tenantNameValue) ? Optional.of(tenantNameValue) : Optional.empty();
        }

        if (tenantName.isPresent()) {
            PlainTenant tenant = new PlainTenant(TenantKey.valueOf(tenantName.get()));
            LOGGER.debug("Init tenant '{}' context", tenant.getTenantKey().getValue());
            getPrivilegedTenantContext().setTenant(tenant);
            return true;
        } else {
            if (isFullyAuthenticated) {
                LOGGER.warn("Can't obtain tenant name from authentication details");
            } else {
                LOGGER.warn("Can't obtain tenant name from non auth http request header '{}'",
                            XmTenantConstants.HTTP_HEADER_TENANT_NAME);
            }
            InterceptorUtil.sendResponse(response, JSON_ERROR_NO_TENANT_SUPPLIED);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        PrivilegedTenantContext privilegedTenantContext = getPrivilegedTenantContext();
        LOGGER.debug("Destroy tenant '{}' context", TenantContextUtils.getRequiredTenantKeyValue(privilegedTenantContext));
        privilegedTenantContext.destroyCurrentContext();
    }

    private PrivilegedTenantContext getPrivilegedTenantContext() {
        return tenantContextHolder.getPrivilegedContext();
    }

}
