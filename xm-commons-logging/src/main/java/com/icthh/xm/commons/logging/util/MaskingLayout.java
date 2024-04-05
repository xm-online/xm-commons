package com.icthh.xm.commons.logging.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaskingLayout extends LayoutBase<ILoggingEvent> {

    private final Layout<ILoggingEvent> layout;
    private final LoggingConfigService loggingConfigService;

    @Override
    public String doLayout(ILoggingEvent event) {
        return maskMessage(layout.doLayout(event));
    }

    private String maskMessage(String message) {
        return loggingConfigService.getMaskingService().maskMessage(message);
    }
}
