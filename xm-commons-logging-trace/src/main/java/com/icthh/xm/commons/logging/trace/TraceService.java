package com.icthh.xm.commons.logging.trace;

import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.logging.trace.TraceService.TraceServiceField.FIELD_NAME;

/**
 * @deprecated org.springframework.cloud.sleuth.instrument.messaging.TracingChannelInterceptor is not supported
 * since Spring Boot 3.x
 */
@Deprecated(forRemoval = true)
@Component
public class TraceService implements LepAdditionalContext<TraceService> {

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
