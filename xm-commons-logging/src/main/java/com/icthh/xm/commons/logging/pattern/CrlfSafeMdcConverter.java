package com.icthh.xm.commons.logging.pattern;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * {@code %X} / {@code %mdc} converter that replaces line breaks in MDC values with a space (CWE-117).
 *
 * <p>Overrides the built-in conversion words in {@code logback-spring.xml}:
 * <pre>
 * &lt;conversionRule conversionWord="X" class="com.icthh.xm.commons.logging.pattern.CrlfSafeMdcConverter"/&gt;
 * &lt;conversionRule conversionWord="mdc" class="com.icthh.xm.commons.logging.pattern.CrlfSafeMdcConverter"/&gt;
 * </pre>
 */
public class CrlfSafeMdcConverter extends MDCConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return CrlfSanitizer.sanitize(super.convert(event));
    }
}
