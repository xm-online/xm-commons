package com.icthh.xm.commons.logging.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TraceWrapper {

    private static final String WRAP_QUOTE_TOKEN = "\"";
    private static final String KAFKA_PROCESSING_TIMER = "kafka.processing.timer";
    private static final String SPAN_NAME_FROM_MESSAGE = "on-message";

    private final Tracer tracer;
    private final Propagator propagator;

    @Autowired
    public TraceWrapper(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public void runWithSpan(ConsumerRecord<?, ?> record, Runnable codeToRun) {
        Span.Builder spanBuilder = propagator.extract(getRecordHeadersMap(record), Map::get);
        Span span = spanBuilder.name(SPAN_NAME_FROM_MESSAGE).start();
        runWithExistingSpan(span, codeToRun);
    }

    public void runWithSpan(Message<?> message, MessageChannel channel, Runnable codeToRun) {
        Span.Builder spanBuilder = propagator.extract(getMessageHeadersMap(message), Map::get);
        Span span = spanBuilder.name(KAFKA_PROCESSING_TIMER).start();
        runWithExistingSpan(span, codeToRun);
    }

    private void runWithExistingSpan(Span existingSpan, Runnable codeToRun) {
        log.trace("Opening span, {}", existingSpan.context().spanId());
        try (Tracer.SpanInScope ws = this.tracer.withSpan(existingSpan)) {
            codeToRun.run();
        } catch (Exception e) {
            existingSpan.error(e);
            throw e;
        } finally {
            log.trace("Closing span, {}", existingSpan.context().spanId());
            existingSpan.end();
        }
    }

    private Map<String, String> getRecordHeadersMap(ConsumerRecord<?, ?> record) {
        Map<String, String> headerMap = new HashMap<>();
        record.headers().forEach(header -> headerMap.put(header.key(), unwrapQuotes(new String(header.value()))));
        return headerMap;
    }

    private Map<String, String> getMessageHeadersMap(Message<?> message) {
        Map<String, String> headerMap = new HashMap<>();
        message.getHeaders().forEach((key, value) -> headerMap.put(key, unwrapQuotes(value.toString())));
        return headerMap;
    }

    public static String unwrapQuotes(final String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        final int wrapLength = WRAP_QUOTE_TOKEN.length();
        if (str.startsWith(WRAP_QUOTE_TOKEN) && str.endsWith(WRAP_QUOTE_TOKEN)) {
            return str.substring(wrapLength, str.length() - wrapLength);
        }
        return str;
    }
}
