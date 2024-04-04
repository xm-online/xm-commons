package com.icthh.xm.commons.logging.configurable;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.MaskingLayout;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
public class LogMaskingConfiguration {

    public LogMaskingConfiguration(LoggingConfigService loggingConfigService) {
        log.info("Init MaskingPatternLayout");
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger("ROOT").iteratorForAppenders().forEachRemaining(it -> {
            if (it instanceof OutputStreamAppender) {
                var appender = (OutputStreamAppender<ILoggingEvent>) it;
                Encoder<ILoggingEvent> encoder = appender.getEncoder();
                if (encoder instanceof LayoutWrappingEncoder) {
                    LayoutWrappingEncoder<ILoggingEvent> layoutEncoder = (LayoutWrappingEncoder<ILoggingEvent>) encoder;
                    Layout<ILoggingEvent> layout = layoutEncoder.getLayout();
                    layoutEncoder.setLayout(new MaskingLayout(layout, loggingConfigService));
                }
            }
        });
    }

}
