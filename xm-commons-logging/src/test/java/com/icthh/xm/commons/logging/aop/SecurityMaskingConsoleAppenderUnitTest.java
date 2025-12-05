package com.icthh.xm.commons.logging.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SecurityMaskingConsoleAppender} that verify
 * masking behavior for log messages containing sensitive keywords.
 */
public class SecurityMaskingConsoleAppenderUnitTest {

    private static final String REPLACEMENT =
            "this log was removed due to potential security breach";

    private LoggingEvent newEvent(String loggerName, String msg) {
        LoggerContext ctx = new LoggerContext();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(loggerName);

        return new LoggingEvent(
                loggerName,
                logger,
                Level.INFO,
                msg,
                null,
                null
        );
    }

    @Test
    public void shouldReplaceMessageForPatternEncoderWhenContainsKeyword() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setKeywords("keyword:, keyword=");
        appender.setReplacementMessage(REPLACEMENT);

        LoggingEvent safe = newEvent("test.pattern.logger", "safe message");
        ILoggingEvent safeResult = appender.applyMasking(safe);

        Assert.assertEquals("safe message", safeResult.getFormattedMessage());

        LoggingEvent sensitive = newEvent("test.pattern.logger", "keyword: 1234");
        ILoggingEvent masked = appender.applyMasking(sensitive);

        Assert.assertEquals(REPLACEMENT, masked.getFormattedMessage());
        Assert.assertFalse(masked.getFormattedMessage().contains("keyword: 1234"));
    }

    @Test
    public void shouldReplaceMessageForJsonEncoderWhenContainsKeyword() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setKeywords("keyword:, keyword=");
        appender.setReplacementMessage(REPLACEMENT);

        LoggingEvent safe = newEvent("test.json.logger", "safe json");
        ILoggingEvent safeResult = appender.applyMasking(safe);

        Assert.assertTrue(safeResult.getFormattedMessage().contains("safe json"));
        Assert.assertFalse(safeResult.getFormattedMessage().contains(REPLACEMENT));

        LoggingEvent sensitive = newEvent("test.json.logger", "keyword=qwerty");
        ILoggingEvent masked = appender.applyMasking(sensitive);

        Assert.assertTrue(masked.getFormattedMessage().contains(REPLACEMENT));
        Assert.assertFalse(masked.getFormattedMessage().contains("keyword=qwerty"));
    }

    @Test
    public void shouldNotMaskWhenNoKeywordsConfigured() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();

        LoggingEvent event = newEvent("test.no.keyword", "keyword: should stay as is");
        ILoggingEvent result = appender.applyMasking(event);

        Assert.assertEquals("keyword: should stay as is", result.getFormattedMessage());
    }

    @Test
    public void shouldReplaceMessageIgnoringCaseForPatternEncoder() {
        SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
        appender.setKeywords("pin=");
        appender.setReplacementMessage(REPLACEMENT);

        LoggingEvent event = newEvent("test.pattern.ignorecase", "PIN=0000");
        ILoggingEvent result = appender.applyMasking(event);

        Assert.assertEquals(REPLACEMENT, result.getFormattedMessage());
        Assert.assertFalse(result.getFormattedMessage().contains("PIN=0000"));
    }
}
