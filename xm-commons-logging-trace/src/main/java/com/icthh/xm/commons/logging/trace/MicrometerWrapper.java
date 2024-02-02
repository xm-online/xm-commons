package com.icthh.xm.commons.logging.trace;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

// todo spring 3.2.0 migration
//  Spring Cloud Sleuth not supported since Spring Boot 3.x - https://docs.spring.io/spring-cloud-sleuth/docs/current-SNAPSHOT/reference/html/
//  Migration gide - https://github.com/micrometer-metrics/tracing/wiki/Spring-Cloud-Sleuth-3.1-Migration-Guide
@Slf4j
@Component
public class MicrometerWrapper {

    private static final String TIMER_NAME = "kafka.processing.timer";
    private static final String SPAN_NAME_FROM_MESSAGE = "on-message";

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @Autowired
    public MicrometerWrapper(Tracer tracer, MeterRegistry meterRegistry) {
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
    }

    public void runWithMicrometer(ConsumerRecord<?, ?> record, Runnable codeToRun) {
//        runWithExistingSpan(kafkaSpan, codeToRun); // TODO
    }

    public void runWithMicrometer(Message<?> message, MessageChannel channel, Runnable codeToRun) {
        Timer.Sample timerSample = Timer.start(meterRegistry); // TODO
        Exception exceptionFromExecution = null;
        try {
            codeToRun.run();
        } finally {
            timerSample.stop(meterRegistry.timer(TIMER_NAME));
        }
    }

    private void runWithExistingSpan(Span existingSpan, Runnable codeToRun) {
        log.trace("Opening span, {}", existingSpan.context().spanId());
        try (Tracer.SpanInScope spanInScope = this.tracer.withSpan(existingSpan)) {
            codeToRun.run();
        } finally {
            log.trace("Closing span, {}", existingSpan.context().spanId());
            existingSpan.end();
        }
    }
}
