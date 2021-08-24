package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepExecutor;
import com.icthh.xm.lep.api.LepExecutorEvent;
import com.icthh.xm.lep.api.LepExecutorListener;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeExecutionEvent;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

@Service
public class LepThreadHelper implements ApplicationListener<ApplicationLepProcessingEvent> {

    private static final String THREAD = "thread";
    private final TenantContextHolder tenantContextHolder;
    private final LepManager lepManager;

    public LepThreadHelper(TenantContextHolder tenantContextHolder,
                           LepManager lepManager) {
        this.tenantContextHolder = tenantContextHolder;
        this.lepManager = lepManager;
    }

    public <T> Future<T> runInThread(ExecutorService executorService, Callable<T> task) {
        return runInThread(executorService, task, SecurityContextHolder.getContext());
    }

    public <T> Future<T> runInThread(ExecutorService executorService, Callable<T> task, SecurityContext securityContext) {
        return executorService.submit(new LepThreadContext<T>(
                lepManager,
                task,
                tenantContextHolder,
                securityContext
        ));
    }

    @Override
    public void onApplicationEvent(ApplicationLepProcessingEvent event) {
        if (event.getLepProcessingEvent() instanceof BeforeExecutionEvent) {
            ScopedContext context = lepManager.getContext(ContextScopes.EXECUTION);
            context.setValue(THREAD, this);
        }
    }

    private static class LepThreadContext<T> implements Callable<T> {

        private final LepManager lepManager;
        private final Callable<T> task;
        private final Tenant tenant;
        private final TenantContext tenantContext;
        private final SecurityContext securityContext;
        private final PrivilegedTenantContext privilegedTenantContext;

        private LepThreadContext(LepManager lepManager, Callable<T> task,
                                 TenantContextHolder tenantContextHolder,
                                 SecurityContext securityContext) {
            this.lepManager = lepManager;
            this.task = task;
            this.privilegedTenantContext = tenantContextHolder.getPrivilegedContext();
            this.tenant = privilegedTenantContext.getTenant()
                    .orElseThrow(() -> new IllegalStateException("Tenant context doesn't have tenant key"));
            this.tenantContext = tenantContextHolder.getContext();
            this.securityContext = securityContext;
        }

        @Override
        public T call() {
            return privilegedTenantContext.execute(tenant, this::runInLepContext);
        }

        @SneakyThrows
        private T runInLepContext() {
            try {
                init();
                return task.call();
            } finally {
                destroy();
            }
        }

        private void init() {
            SecurityContextHolder.setContext(securityContext);
            XmAuthenticationContext authContext = new SpringSecurityXmAuthenticationContextHolder().getContext();
            lepManager.beginThreadContext(threadContext -> {
                threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContext);
                threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContext);
            });
        }

        private void destroy() {
            lepManager.endThreadContext();
            SecurityContextHolder.clearContext();
        }
    }

}
