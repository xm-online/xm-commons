package com.icthh.xm.commons.logging.trace;

import brave.Span;
import brave.Tracer;
import brave.kafka.clients.KafkaTracing;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.instrument.messaging.TracingChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SleuthWrapper {

    private static final String SPAN_NAME_FROM_MESSAGE = "on-message";

    private final Tracer tracer;
    private KafkaTracing kafkaTracing;
    private TracingChannelInterceptor tracingChannelInterceptor;

    public SleuthWrapper(Tracer tracer) {
        this.tracer = tracer;
    }

    @Autowired(required = false)
    public void setKafkaTracing(KafkaTracing kafkaTracing) {
        this.kafkaTracing = kafkaTracing;
    }

    @Autowired(required = false)
    public void setTracingChannelInterceptor(TracingChannelInterceptor tracingChannelInterceptor) {
        this.tracingChannelInterceptor = tracingChannelInterceptor;
    }

    public void runWithSleuth(ConsumerRecord<?, ?> record, Runnable codeToRun) {
        Span kafkaSpan = kafkaTracing.nextSpan(record).name(SPAN_NAME_FROM_MESSAGE).start();
        runWithExistingSpan(kafkaSpan, codeToRun);
    }

    public void runWithSleuth(Message<?> message, Runnable codeToRun) {
        Span messageSpan = tracingChannelInterceptor.nextSpan(message).name(SPAN_NAME_FROM_MESSAGE).start();
        runWithExistingSpan(messageSpan, codeToRun);
    }

    private void runWithExistingSpan(Span existingSpan, Runnable codeToRun) {
        log.trace("Opening span, {}", existingSpan.context().spanIdString());
        try (Tracer.SpanInScope spanInScope = this.tracer.withSpanInScope(existingSpan)) {
            codeToRun.run();
        } finally {
            log.trace("Closing span, {}", existingSpan.context().spanIdString());
            existingSpan.finish();
        }
    }
}
