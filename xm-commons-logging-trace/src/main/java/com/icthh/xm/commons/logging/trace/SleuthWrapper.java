package com.icthh.xm.commons.logging.trace;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.messaging.TracingChannelInterceptor;
import org.springframework.cloud.sleuth.propagation.Propagator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SleuthWrapper {

    private static final String SPAN_NAME_FROM_MESSAGE = "on-message";
    private static final String SPAN_ID_UNKNOWN = "<unknown>";

    private static final Propagator.Getter<ConsumerRecord<?, ?>> KAFKA_GETTER =
        (rec, key) -> {
            var header = rec.headers().lastHeader(key);
            return Optional.ofNullable(header)
                    .map(h -> StringUtils.newStringUtf8(h.value()))
                    .orElse(null);
        };

    private final Tracer tracer;
    private final Propagator propagator;
    private TracingChannelInterceptor tracingChannelInterceptor;

    public SleuthWrapper(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Autowired(required = false)
    public void setTracingChannelInterceptor(TracingChannelInterceptor tracingChannelInterceptor) {
        this.tracingChannelInterceptor = tracingChannelInterceptor;
    }

    public void runWithSleuth(ConsumerRecord<?, ?> record, Runnable codeToRun) {
        Span.Builder extracted = propagator.extract(record, KAFKA_GETTER);
        Span kafkaSpan = extracted.name(SPAN_NAME_FROM_MESSAGE).start();
        runWithExistingSpan(kafkaSpan, codeToRun);
    }

    public void runWithSleuth(Message<?> message, MessageChannel channel, Runnable codeToRun) {
        if (this.tracingChannelInterceptor == null) {
            codeToRun.run();
            return;
        }
        tracingChannelInterceptor.postReceive(message, channel);
        Exception exceptionFromExecution = null;
        try {
            codeToRun.run();
        } catch (Exception e) {
            exceptionFromExecution = e;
        } finally {
            tracingChannelInterceptor.afterReceiveCompletion(message, channel, exceptionFromExecution);
        }
    }

    private void runWithExistingSpan(Span existingSpan, Runnable codeToRun) {

        String spanId = existingSpan.context() != null
            ? existingSpan.context().spanId()
            : SPAN_ID_UNKNOWN;

        log.trace("Opening span, {}", spanId);
        try (Tracer.SpanInScope spanInScope = this.tracer.withSpan(existingSpan)) {
            codeToRun.run();
        } finally {
            log.trace("Closing span, {}", spanId);
            existingSpan.end();
        }
    }
}
