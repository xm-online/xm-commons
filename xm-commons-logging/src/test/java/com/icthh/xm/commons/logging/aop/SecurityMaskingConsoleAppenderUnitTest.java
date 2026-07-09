package com.icthh.xm.commons.logging.aop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SecurityMaskingConsoleAppender} that verify
 * masking behavior for log messages containing sensitive keywords.
 */
public class SecurityMaskingConsoleAppenderUnitTest {

    private static final String REPLACEMENT =
            "this log was removed due to potential security breach";

    private LoggingEvent newEvent(String loggerName, String msg) {
        LoggerContext ctx = new LoggerContext();
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(ctx);
        event.setLoggerName(loggerName);
        event.setLevel(Level.INFO);
        event.setMessage(msg);
        event.setThreadName(Thread.currentThread().getName());
        event.setTimeStamp(System.currentTimeMillis());
        event.setMDCPropertyMap(Collections.emptyMap());
        return event;
    }

    @Test
    public void shouldReplaceMessageForPatternEncoderWhenContainsKeyword() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setKeywords("keyword:, keyword=");
        appender.setReplacementMessage(REPLACEMENT);

        LoggingEvent safe = newEvent("test.pattern.logger", "safe message");
        var safeResult = appender.applyMasking(safe);

        assertThat(safeResult.getFormattedMessage(), is("safe message"));

        LoggingEvent sensitive = newEvent("test.pattern.logger", "keyword: 1234");
        var masked = appender.applyMasking(sensitive);

        assertThat(masked.getFormattedMessage(), is(REPLACEMENT));
        assertThat(masked.getFormattedMessage(), not(containsString("keyword: 1234")));
    }

    @Test
    public void shouldReplaceMessageForJsonEncoderWhenContainsKeyword() {
        LoggerContext ctx = new LoggerContext();

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(ctx);
        encoder.start();

        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setContext(ctx);
        appender.setKeywords("keyword:, keyword=");
        appender.setReplacementMessage(REPLACEMENT);
        appender.setEncoder(encoder);

        String safeJson = appendAndCapture(appender, newEvent("test.json.logger", "safe json"));
        assertThat(safeJson, containsString("safe json"));
        assertThat(safeJson, not(containsString(REPLACEMENT)));

        String maskedJson = appendAndCapture(appender, newEvent("test.json.logger", "keyword=qwerty"));
        assertThat(maskedJson, containsString(REPLACEMENT));
        assertThat(maskedJson, not(containsString("keyword=qwerty")));
    }

    private String appendAndCapture(SecurityMaskingConsoleAppender appender, LoggingEvent event) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appender.start();
        appender.setOutputStream(out);
        try {
            appender.doAppend(event);
        } finally {
            appender.stop();
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void shouldNotMaskWhenNoKeywordsConfigured() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();

        LoggingEvent event = newEvent("test.no.keyword", "keyword: should stay as is");
        var result = appender.applyMasking(event);

        assertThat(result.getFormattedMessage(), is("keyword: should stay as is"));
    }

    @Test
    public void shouldNotMaskWhenReplacementMessageMissing() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setKeywords("pin=");

        LoggingEvent event = newEvent("test.no.replacement", "pin=0000");
        var result = appender.applyMasking(event);

        assertThat(result.getFormattedMessage(), is("pin=0000"));
    }

    @Test
    public void shouldReplaceMessageIgnoringCaseForPatternEncoder() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setKeywords("pin=");
        appender.setReplacementMessage(REPLACEMENT);

        LoggingEvent event = newEvent("test.pattern.ignorecase", "PIN=0000");
        var result = appender.applyMasking(event);

        assertThat(result.getFormattedMessage(), is(REPLACEMENT));
        assertThat(result.getFormattedMessage(), not(containsString("PIN=0000")));
    }
}
