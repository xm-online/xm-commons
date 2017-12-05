package com.icthh.xm.commons.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.UtilityClass;
import net.logstash.logback.appender.LogstashSocketAppender;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LogstashConfigurer} class.
 */
@UtilityClass
public final class LogstashConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogstashConfigurer.class);

    @Data
    @AllArgsConstructor
    public static class XmLogstashConfig {
        private String appName;
        private int appPort;
        private String instanceId;
        private String logstashHost;
        private int logstashPort;
        private int logstashQueueSize;
    }

    public static void initLogstash(XmLogstashConfig config) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        addLogstashAppender(context, config);

        // Add context listener
        context.addListener(new LogbackLoggerContextListener(context, config));
    }

    private static void addLogstashAppender(LoggerContext context, XmLogstashConfig config) {
        LOGGER.info("Initializing Logstash logging");

        LogstashSocketAppender logstashAppender = new LogstashSocketAppender();
        logstashAppender.setName("LOGSTASH");
        logstashAppender.setContext(context);
        String customFields = "{\"app_name\":\"" + config.getAppName() + "\",\"app_port\":\"" + config.getAppPort()
            + "\",\"instance_id\":\"" + config.getInstanceId() + "\"}";

        // Set the Logstash appender config from JHipster properties
        logstashAppender.setSyslogHost(config.getLogstashHost());
        logstashAppender.setPort(config.getLogstashPort());
        logstashAppender.setCustomFields(customFields);

        // Limit the maximum length of the forwarded stacktrace so that it won't exceed the 8KB UDP limit of logstash
        ShortenedThrowableConverter throwableConverter = new ShortenedThrowableConverter();
        throwableConverter.setMaxLength(7500);
        throwableConverter.setRootCauseFirst(true);
        logstashAppender.setThrowableConverter(throwableConverter);

        logstashAppender.start();

        // Wrap the appender in an Async appender for performance
        AsyncAppender asyncLogstashAppender = new AsyncAppender();
        asyncLogstashAppender.setContext(context);
        asyncLogstashAppender.setName("ASYNC_LOGSTASH");
        asyncLogstashAppender.setQueueSize(config.getLogstashQueueSize());
        asyncLogstashAppender.addAppender(logstashAppender);
        asyncLogstashAppender.start();

        context.getLogger("ROOT").addAppender(asyncLogstashAppender);
    }

    /**
     * Logback configuration is achieved by configuration file and API.
     * When configuration file change is detected, the configuration is reset.
     * This listener ensures that the programmatic configuration is also re-applied after reset.
     */
    static class LogbackLoggerContextListener extends ContextAwareBase implements LoggerContextListener {

        private final XmLogstashConfig config;

        private LogbackLoggerContextListener(LoggerContext context, XmLogstashConfig config) {
            setContext(context);
            this.config = config;
        }

        @Override
        public boolean isResetResistant() {
            return true;
        }

        @Override
        public void onStart(LoggerContext context) {
            addLogstashAppender(context, config);
        }

        @Override
        public void onReset(LoggerContext context) {
            addLogstashAppender(context, config);
        }

        @Override
        public void onStop(LoggerContext context) {
            // Nothing to do.
        }

        @Override
        public void onLevelChange(ch.qos.logback.classic.Logger logger, Level level) {
            // Nothing to do.
        }
    }

}
