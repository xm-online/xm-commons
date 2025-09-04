package com.icthh.xm.commons.lep.spring;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraceCallableDecorator<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final TraceContext traceContext;
    private final CurrentTraceContext currentTraceContext;

    public TraceCallableDecorator(Callable<T> delegate,
                                  TraceContext traceContext,
                                  CurrentTraceContext currentTraceContext) {
        this.delegate = delegate;
        this.traceContext = traceContext;
        this.currentTraceContext = currentTraceContext;
    }

    @Override
    public T call() throws Exception {
        try (CurrentTraceContext.Scope scope =
                 traceContext != null ? currentTraceContext.maybeScope(traceContext) : () -> {
                 }) {
            return delegate.call();
        }
    }
}
