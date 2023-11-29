package com.icthh.xm.commons.logging.trace;

import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.instrument.web.mvc.TracingClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.logging.trace.TraceService.TraceServiceField.FIELD_NAME;

@Component
@RequiredArgsConstructor
public class TraceService implements LepAdditionalContext<TraceService> {

    private final TracingClientHttpRequestInterceptor tracingClientHttpRequestInterceptor;

    public TracingClientHttpRequestInterceptor getTraceInterceptor() {
        return tracingClientHttpRequestInterceptor;
    }

    @Override
    public String additionalContextKey() {
        return FIELD_NAME;
    }

    @Override
    public TraceService additionalContextValue() {
        return this;
    }

    @Override
    public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
        return TraceServiceField.class;
    }

    public interface TraceServiceField extends LepAdditionalContextField {
        String FIELD_NAME = "traceService";
        default TraceService getTraceService() {
            return (TraceService)get(FIELD_NAME);
        }
    }
}
