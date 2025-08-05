package com.icthh.xm.commons.metric;

import static java.lang.Boolean.TRUE;
import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;

import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableMetrics(proxyTargetClass = true)
public class MetricsConfiguration extends MetricsConfigurerAdapter {

    private final Logger log = LoggerFactory.getLogger(MetricsConfiguration.class);

    private final MeterRegistry meterRegistry;
    private final KafkaAdmin kafkaAdmin;

   @Value("${application.kafkaMetric.enabled:false}")
    private Boolean kafkaMetricEnabled;
    @Value("${application.kafkaMetric.connectionTimeoutTopic:#{null}}")
    private Integer connectionTimeoutTopic;
    @Value("${application.kafkaMetric.metricTopics:#{null}}")
    private List<String> metricTopics;

    public MetricsConfiguration(MeterRegistry meterRegistry, KafkaAdmin kafkaAdmin) {
        this.meterRegistry = meterRegistry;
        this.kafkaAdmin = kafkaAdmin;
    }

    /**
     * Init commons metrics
     */
    @PostConstruct
    public void init() {
        log.debug("Registering JVM gauges");
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new OperatingSystemMetrics(getOperatingSystemMXBean()).bindTo(meterRegistry);

        if (TRUE.equals(kafkaMetricEnabled)) {
            new KafkaMetrics(kafkaAdmin, connectionTimeoutTopic, metricTopics).bindTo(meterRegistry);
        }
    }
}
