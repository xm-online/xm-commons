package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * The {@link XmLoggingInterceptor} class.
 */
public class XmLoggingInterceptor implements HandlerInterceptor {

    private final XmAuthenticationContextHolder authenticationContextHolder;
    private final TenantContextHolder tenantContextHolder;

    public XmLoggingInterceptor(XmAuthenticationContextHolder authenticationContextHolder,
                                TenantContextHolder tenantContextHolder) {
        this.authenticationContextHolder = authenticationContextHolder;
        this.tenantContextHolder = tenantContextHolder;
    }

    private String getLogin() {
        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        return authContext.getLogin().orElse("[unknown]");
    }

    private String getTenantName() {
        Optional<TenantKey> tenantName = tenantContextHolder.getContext().getTenantKey();
        return tenantName.isPresent() ? tenantName.get().getValue() : "[no tenant]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MdcUtils.putRid(MdcUtils.generateRid() + ":" + getLogin() + ":" + getTenantName());
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
        MdcUtils.clear();
    }

}
