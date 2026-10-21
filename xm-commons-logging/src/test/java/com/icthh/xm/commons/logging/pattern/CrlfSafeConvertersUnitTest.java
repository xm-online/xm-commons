package com.icthh.xm.commons.logging.pattern;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CrlfSafeConvertersUnitTest {

    private static final String CONFIG = """
        <configuration>
            <conversionRule conversionWord="m" converterClass="com.icthh.xm.commons.logging.pattern.CrlfSafeMessageConverter"/>
            <conversionRule conversionWord="msg" converterClass="com.icthh.xm.commons.logging.pattern.CrlfSafeMessageConverter"/>
            <conversionRule conversionWord="message" converterClass="com.icthh.xm.commons.logging.pattern.CrlfSafeMessageConverter"/>
            <conversionRule conversionWord="X" converterClass="com.icthh.xm.commons.logging.pattern.CrlfSafeMdcConverter"/>
            <conversionRule conversionWord="mdc" converterClass="com.icthh.xm.commons.logging.pattern.CrlfSafeMdcConverter"/>
        </configuration>
        """;

    private LoggerContext context;

    @BeforeEach
    @SneakyThrows
    public void before() {
        context = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(new ByteArrayInputStream(CONFIG.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testMessageWordsAreOverridden() {
        LoggingEvent event = event("user: {}", Map.of(), "admin\r\nINFO forged entry");

        assertEquals("user: admin INFO forged entry|user: admin INFO forged entry|user: admin INFO forged entry",
            format("%m|%msg|%message", event));
    }

    @Test
    public void testFormatLineBreaksAreReplaced() {
        assertEquals("a b", format("%msg", event("a\n\nb", Map.of())));
    }

    @Test
    public void testSpacesAndTabsAreKept() {
        assertEquals("a  \tb", format("%msg", event("a  \tb", Map.of())));
    }

    @Test
    public void testMdcValuesAreSanitized() {
        LoggingEvent event = event("text", Map.of("tenant", "XM\nINFO forged"));

        assertEquals("XM INFO forged|XM INFO forged|tenant=XM INFO forged",
            format("%X{tenant}|%mdc{tenant}|%X", event));
    }

    @Test
    public void testMdcDefaultValue() {
        assertEquals("none", format("%X{rid:-none}", event("text", Map.of())));
    }

    @Test
    public void testOtherConvertersAreUntouched() {
        assertEquals("INFO text", format("%level %msg", event("text", Map.of())));
    }

    @Test
    public void testSanitizer() {
        assertNull(CrlfSanitizer.sanitize(null));
        assertEquals("", CrlfSanitizer.sanitize(""));
        assertEquals("no breaks", CrlfSanitizer.sanitize("no breaks"));
        assertEquals("a b c d e ", CrlfSanitizer.sanitize("a\u0085b c d\u000B\fe\r\n"));
    }

    private String format(String pattern, LoggingEvent event) {
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern(pattern);
        layout.start();
        return layout.doLayout(event);
    }

    private LoggingEvent event(String message, Map<String, String> mdc, Object... args) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setLoggerName("test");
        event.setLevel(Level.INFO);
        event.setMessage(message);
        event.setArgumentArray(args.length == 0 ? null : args);
        event.setMDCPropertyMap(mdc);
        return event;
    }
}
