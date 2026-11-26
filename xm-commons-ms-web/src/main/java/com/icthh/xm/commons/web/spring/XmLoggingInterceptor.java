package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link XmLoggingInterceptor} class.
 */
public class XmLoggingInterceptor implements AsyncHandlerInterceptor {

    public static final String IMPERSONATE_INBOUND_LOGIN = "impersonateInboundLogin";

    private static final String ACTIVE_REQUEST_ATTR = XmLoggingInterceptor.class.getName() + ".ACTIVE";
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
        // count the request once: async/error redispatches reuse the same request object
        if (request.getAttribute(ACTIVE_REQUEST_ATTR) == null) {
            request.setAttribute(ACTIVE_REQUEST_ATTR, new AtomicBoolean(true));
            MdcUtils.setActiveCount(ACTIVE_REQUESTS.incrementAndGet());
        } else {
            MdcUtils.setActiveCount(ACTIVE_REQUESTS.get());
        }
        return true;
    }

    /**
     * Called instead of {@link #afterCompletion} when async handling starts. The request stays counted
     * until the async lifecycle ends (complete, timeout or error); MDC is cleared before the container
     * thread is released back to the pool.
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request,
                                               HttpServletResponse response,
                                               Object handler) {
        if (request.isAsyncStarted()) {
            request.getAsyncContext().addListener(new ReleaseOnCompleteListener(request));
        }
        MdcUtils.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        if (!request.isAsyncStarted()) {
            release(request);
        }
        MdcUtils.clear();
    }

    private static void release(ServletRequest request) {
        if (request.getAttribute(ACTIVE_REQUEST_ATTR) instanceof AtomicBoolean counted
            && counted.compareAndSet(true, false)) {
            ACTIVE_REQUESTS.decrementAndGet();
        }
    }

    private record ReleaseOnCompleteListener(ServletRequest request) implements AsyncListener {

        @Override
        public void onComplete(AsyncEvent event) {
            release(request);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            // onComplete follows timeout handling
        }

        @Override
        public void onError(AsyncEvent event) {
            // onComplete follows error handling
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            // async restarted on redispatch: keep listening on the new context
            event.getAsyncContext().addListener(this);
        }
    }

}
