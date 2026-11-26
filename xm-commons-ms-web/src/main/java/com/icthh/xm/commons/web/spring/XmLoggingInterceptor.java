package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link XmLoggingInterceptor} class.
 */
public class XmLoggingInterceptor implements AsyncHandlerInterceptor {

    public static final String IMPERSONATE_INBOUND_LOGIN = "impersonateInboundLogin";

    private static final AtomicInteger ACTIVE_REQUESTS = new AtomicInteger();

    private final XmAuthenticationContextHolder authenticationContextHolder;
    private final TenantContextHolder tenantContextHolder;

    public XmLoggingInterceptor(XmAuthenticationContextHolder authenticationContextHolder,
                                TenantContextHolder tenantContextHolder) {
        this.authenticationContextHolder = authenticationContextHolder;
        this.tenantContextHolder = tenantContextHolder;
    }

    private String getLogin() {
        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        Optional<String> impersonateLogin = authContext.getAdditionalDetailsValue(IMPERSONATE_INBOUND_LOGIN);
        String userLogin = authContext.getLogin().orElse("[unknown]");
        return impersonateLogin.map(il -> il + ":" + userLogin).orElse(userLogin);
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
        MdcUtils.setActiveCount(ACTIVE_REQUESTS.incrementAndGet());
        return true;
    }

    /**
     * Called instead of {@link #afterCompletion} when async handling starts;
     * {@link #preHandle} runs again on the async dispatch, so decrement here to keep the counter balanced.
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request,
                                               HttpServletResponse response,
                                               Object handler) {
        ACTIVE_REQUESTS.decrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        ACTIVE_REQUESTS.decrementAndGet();
        MdcUtils.clear();
    }

}
