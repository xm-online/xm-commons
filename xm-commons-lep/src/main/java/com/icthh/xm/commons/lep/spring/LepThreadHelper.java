package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.LepExecutorResolver;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
public class LepThreadHelper {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;

    public LepThreadHelper(TenantContextHolder tenantContextHolder,
                           LepManagementService lepManagementService) {
        this.tenantContextHolder = tenantContextHolder;
        this.lepManagementService = lepManagementService;
    }

    public <T> Future<T> runInThread(ExecutorService executorService, Callable<T> task) {
        return runInThread(executorService, task, SecurityContextHolder.getContext());
    }

    public <T> Future<T> runInThread(ExecutorService executorService, Callable<T> task, SecurityContext securityContext) {
        return executorService.submit(new LepThreadContext<T>(
            lepManagementService,
            task,
            tenantContextHolder,
            securityContext
        ));
    }

    private static class LepThreadContext<T> implements Callable<T> {

        private final LepManagementService lepManagementService;
        private final LepExecutorResolver lepExecutorResolver;
        private final Callable<T> task;
        private final Tenant tenant;
        private final SecurityContext securityContext;
        private final PrivilegedTenantContext privilegedTenantContext;

        private LepThreadContext(LepManagementService lepManagementService, Callable<T> task,
                                 TenantContextHolder tenantContextHolder,
                                 SecurityContext securityContext) {
            this.lepManagementService = lepManagementService;
            this.lepExecutorResolver = lepManagementService.getCurrentLepExecutorResolver();
            this.task = task;
            this.privilegedTenantContext = tenantContextHolder.getPrivilegedContext();
            this.tenant = privilegedTenantContext.getTenant()
                    .orElseThrow(() -> new IllegalStateException("Tenant context doesn't have tenant key"));
            this.securityContext = securityContext;
        }

        @Override
        public T call() {
            return privilegedTenantContext.execute(tenant, this::runInLepContext);
        }

        @SneakyThrows
        private T runInLepContext() {
            try (var threadContext = lepManagementService.beginThreadContext(lepExecutorResolver)){
                SecurityContextHolder.setContext(securityContext);
                return task.call();
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

}
