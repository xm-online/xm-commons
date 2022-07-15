package com.icthh.xm.commons.logging.trace;

import brave.spring.web.TracingClientHttpRequestInterceptor;
import com.icthh.xm.commons.lep.spring.ApplicationLepProcessingEvent;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TraceService implements ApplicationListener<ApplicationLepProcessingEvent> {

    private final LepManager lepManager;
    private final TracingClientHttpRequestInterceptor tracingClientHttpRequestInterceptor;

    @Override
    public void onApplicationEvent(ApplicationLepProcessingEvent event) {
        if (event.getLepProcessingEvent() instanceof LepProcessingEvent.BeforeExecutionEvent) {
            ScopedContext context = this.lepManager.getContext(ContextScopes.EXECUTION);
            context.setValue("traceService", this);
        }
    }

    public TracingClientHttpRequestInterceptor getTraceInterceptor() {
        return tracingClientHttpRequestInterceptor;
    }
}