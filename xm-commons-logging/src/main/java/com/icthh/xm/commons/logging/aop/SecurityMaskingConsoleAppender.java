package com.icthh.xm.commons.logging.aop;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Setter;

/**
 * Console appender that performs security masking of log messages based on configured keywords.
 * <p>
 * If a log message contains any of the defined keywords (case-insensitive),
 * the entire message will be replaced with the configured {@code replacementMessage}.
 * This prevents sensitive information (PIN, PUK, passwords, tokens, etc.)
 * from appearing in logs.
 * <p>
 * Typical configuration (in logback-spring.xml):
 *
 * <pre>{@code
 * <appender name="MASKED_CONSOLE" class="com.icthh.xm.commons.logging.aop.SecurityMaskingConsoleAppender">
 *     <keywords>pin=, puk:, password, authToken</keywords>
 *     <replacementMessage>[MASKED]</replacementMessage>
 *     <encoder>
 *         <pattern>%msg%n</pattern>
 *     </encoder>
 * </appender>
 * }</pre>
 *
 * This appender wraps the original {@link ILoggingEvent} into {@link MaskedLoggingEvent}
 * when masking is required.
 */
public class SecurityMaskingConsoleAppender extends ConsoleAppender<ILoggingEvent> {

    /**
     * List of case-insensitive keywords which, if found in a log message,
     * will trigger masking.
     */
    private final List<String> keywords = new ArrayList<>();

    /**
     * Message that will replace the original log message when masking is applied.
     * Must be configured explicitly.
     * -- SETTER --
     *  Sets the message that will be logged instead of the original message
     *  when masking is triggered.
     *
     * @param replacementMessage replacement text (can be null if masking is disabled)

     */
    @Setter
    private String replacementMessage;

    /**
     * Adds a single keyword to the internal keyword list.
     * Keywords are stored in lowercase to allow case-insensitive comparison.
     *
     * @param keyword keyword to add (ignored if null or blank)
     */
    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            keywords.add(keyword.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Configures masking keywords from a comma-separated string..
     * Example: {@code "pin=, puk:, password"}
     *
     * @param keywords comma-separated keywords list.
     */
    public void setKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return;
        }

        for (String k : keywords.split(",")) {
            addKeyword(k.trim());
        }
    }

    /**
     * Wraps the incoming event in {@link MaskedLoggingEvent} if masking is needed.
     * Otherwise, returns the original event.
     *
     * @param eventObject original logging event
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        ILoggingEvent toLog = applyMasking(eventObject);
        super.append(toLog);
    }

    /**
     * Applies masking logic to the provided logging event.
     * This method is extracted for easier unit testing.
     *
     * @param original the original event
     * @return masked event, or original event if no keyword matched
     */
    ILoggingEvent applyMasking(ILoggingEvent original) {
        if (original == null) {
            return null;
        }

        String msg = original.getFormattedMessage();
        if (msg == null) {
            msg = original.getMessage();
        }

        if (containsSensitive(msg)) {
            return new MaskedLoggingEvent(original, replacementMessage);
        }

        return original;
    }

    /**
     * Checks whether the message contains any of the configured keywords.
     * Comparison is case-insensitive.
     *
     * @param msg message to check
     * @return true if message contains a sensitive keyword
     */
    private boolean containsSensitive(String msg) {
        if (msg == null || keywords.isEmpty()) {
            return false;
        }

        String lowerMsg = msg.toLowerCase(Locale.ROOT);

        for (String keyword : keywords) {
            if (lowerMsg.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
