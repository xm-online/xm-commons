package com.icthh.xm.commons.lep.spring;

import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

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
