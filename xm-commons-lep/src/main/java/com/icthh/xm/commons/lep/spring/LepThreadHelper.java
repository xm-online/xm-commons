package com.icthh.xm.commons.lep.spring;

import brave.Span;
import brave.Tracer;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
public class LepThreadHelper {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;
    private final Tracer tracer;
    private final CurrentTraceContext currentTraceContext;

    public LepThreadHelper(TenantContextHolder tenantContextHolder,
                           Tracer tracer,
                           CurrentTraceContext currentTraceContext,
                           LepManagementService lepManagementService) {
        this.tenantContextHolder = tenantContextHolder;
        this.lepManagementService = lepManagementService;
        this.tracer = tracer;
        this.currentTraceContext = currentTraceContext;
    }

    public <T> Future<T> runInThread(ExecutorService executorService, Callable<T> task) {
        String rid = MdcUtils.getRid();
        return executorService.submit(
            new TenantCallableDecorator<>(
                lepManagementService,
                task,
                tenantContextHolder,
                SecurityContextHolder.getContext(),
                rid
            )
        );
    }

    public <T> Future<T> runInThreadWithTracing(ExecutorService executorService, Callable<T> task) {
        TraceContext traceContext = Optional.ofNullable(tracer.currentSpan())
            .map(Span::context)
            .orElse(null);
        String rid = MdcUtils.getRid();
        return executorService.submit(
            new TraceCallableDecorator<>(
                new TenantCallableDecorator<>(lepManagementService,
                    task,
                    tenantContextHolder,
                    SecurityContextHolder.getContext(),
                    rid),
                traceContext,
                currentTraceContext
            )
        );
    }
}
