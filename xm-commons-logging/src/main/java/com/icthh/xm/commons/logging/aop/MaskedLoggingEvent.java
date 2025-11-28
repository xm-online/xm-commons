package com.icthh.xm.commons.logging.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;

import java.util.Map;

/**
 * {@link ILoggingEvent} wrapper that overrides only the message-related fields.
 * Everything else is delegated to the original event.
 */
public class MaskedLoggingEvent implements ILoggingEvent {

    private final ILoggingEvent delegate;
    private final String maskedMessage;

    public MaskedLoggingEvent(ILoggingEvent delegate, String maskedMessage) {
        this.delegate = delegate;
        this.maskedMessage = maskedMessage;
    }

    @Override
    public String getThreadName() {
        return delegate.getThreadName();
    }

    @Override
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public String getMessage() {
        return maskedMessage;
    }

    @Override
    public Object[] getArgumentArray() {
        return new Object[0]; // prevent formatter reuse
    }

    @Override
    public String getFormattedMessage() {
        return maskedMessage;
    }

    @Override
    public String getLoggerName() {
        return delegate.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return delegate.getLoggerContextVO();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return delegate.getThrowableProxy();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return delegate.getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return delegate.hasCallerData();
    }

    @Override
    public Marker getMarker() {
        return delegate.getMarker();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return delegate.getMDCPropertyMap();
    }

    /**
     * @deprecated SLF4J 1.x requires this method to stay.
     */
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public Map<String, String> getMdc() {
        return delegate.getMdc();
    }

    @Override
    public long getTimeStamp() {
        return delegate.getTimeStamp();
    }

    @Override
    public void prepareForDeferredProcessing() {
        delegate.prepareForDeferredProcessing();
    }
}
