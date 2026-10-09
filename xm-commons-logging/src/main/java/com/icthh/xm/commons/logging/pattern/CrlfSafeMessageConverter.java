package com.icthh.xm.commons.logging.pattern;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * {@code %msg} converter that replaces line breaks in the formatted message with a space (CWE-117).
 *
 * <p>Overrides the built-in conversion words in {@code logback-spring.xml}, so every pattern
 * that uses them is covered, whatever logger produced the event:
 * <pre>
 * &lt;conversionRule conversionWord="m" class="com.icthh.xm.commons.logging.pattern.CrlfSafeMessageConverter"/&gt;
 * &lt;conversionRule conversionWord="msg" class="com.icthh.xm.commons.logging.pattern.CrlfSafeMessageConverter"/&gt;
 * &lt;conversionRule conversionWord="message" class="com.icthh.xm.commons.logging.pattern.CrlfSafeMessageConverter"/&gt;
 * </pre>
 */
public class CrlfSafeMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return CrlfSanitizer.sanitize(super.convert(event));
    }
}
