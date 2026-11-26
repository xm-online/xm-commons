package com.icthh.xm.commons.web.spring;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmLoggingInterceptorUnitTest {

    private final XmLoggingInterceptor interceptor = new XmLoggingInterceptor(
        new SpringSecurityXmAuthenticationContextHolder(), new DefaultTenantContextHolder());
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearMdc() {
        MdcUtils.clear();
    }

    @Test
    void syncRequestIsCountedAndReleased() {
        int base = currentActive();
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertEquals(base + 1, activeAfterPreHandle(request));
        interceptor.afterCompletion(request, response, null, null);

        assertEquals(base, currentActive());
    }

    @Test
    void asyncRequestStaysCountedAcrossRedispatchAndIsReleasedOnce() {
        int base = currentActive();
        MockHttpServletRequest request = startAsyncRequest();
        MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();

        // still counted while waiting for the async result
        assertEquals(base + 1, currentActive());

        // async redispatch: not counted again
        request.setAsyncStarted(false);
        request.setDispatcherType(DispatcherType.ASYNC);
        assertEquals(base + 1, activeAfterPreHandle(request));
        interceptor.afterCompletion(request, response, null, null);
        asyncContext.complete();

        // released exactly once
        assertEquals(base, currentActive());
    }

    @Test
    void asyncRequestCompletedWithoutRedispatchIsReleased() {
        int base = currentActive();
        MockHttpServletRequest request = startAsyncRequest();

        ((MockAsyncContext) request.getAsyncContext()).complete();

        assertEquals(base, currentActive());
    }

    private MockHttpServletRequest startAsyncRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAsyncSupported(true);
        interceptor.preHandle(request, response, null);
        request.startAsync();
        interceptor.afterConcurrentHandlingStarted(request, response, null);
        return request;
    }

    /** Active count excluding a probe request, which is released afterwards. */
    private int currentActive() {
        MockHttpServletRequest probe = new MockHttpServletRequest();
        int active = activeAfterPreHandle(probe) - 1;
        interceptor.afterCompletion(probe, response, null, null);
        return active;
    }

    private int activeAfterPreHandle(MockHttpServletRequest request) {
        interceptor.preHandle(request, response, null);
        return Integer.parseInt(MdcUtils.getActiveCount());
    }
}
