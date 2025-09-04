package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.api.LepExecutorResolver;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;

public class TenantCallableDecorator<T> implements Callable<T> {

    private final LepManagementService lepManagementService;
    private final LepExecutorResolver lepExecutorResolver;
    private final Callable<T> task;
    private final Tenant tenant;
    private final String rid;
    private final SecurityContext securityContext;
    private final PrivilegedTenantContext privilegedTenantContext;

    public TenantCallableDecorator(LepManagementService lepManagementService,
                                   Callable<T> delegate,
                                   TenantContextHolder tenantContextHolder,
                                   SecurityContext securityContext,
                                   String rid) {
        this.lepManagementService = lepManagementService;
        this.lepExecutorResolver = lepManagementService.getCurrentLepExecutorResolver();
        this.task = delegate;
        this.privilegedTenantContext = tenantContextHolder.getPrivilegedContext();
        this.tenant = privilegedTenantContext.getTenant()
            .orElseThrow(() -> new IllegalStateException("Tenant context doesn't have tenant key"));
        this.securityContext = securityContext;
        this.rid = rid;
    }

    @Override
    public T call() {
        return privilegedTenantContext.execute(tenant, this::runInLepContext);
    }

    @SneakyThrows
    private T runInLepContext() {
        try (var threadContext = lepManagementService.beginThreadContext(lepExecutorResolver)) {
            SecurityContextHolder.setContext(securityContext);
            MdcUtils.putRid(rid);
            return task.call();
        } finally {
            MdcUtils.removeRid();
            SecurityContextHolder.clearContext();
        }
    }
}
