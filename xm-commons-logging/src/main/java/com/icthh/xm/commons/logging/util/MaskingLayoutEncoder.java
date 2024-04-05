package com.icthh.xm.commons.logging.util;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaskingLayoutEncoder extends PatternLayoutEncoder {

    private final LoggingConfigService loggingConfigService;

    @Override
    public void start() {
        super.start();
        this.layout = new MaskingLayout(this.layout, loggingConfigService);
    }
}
