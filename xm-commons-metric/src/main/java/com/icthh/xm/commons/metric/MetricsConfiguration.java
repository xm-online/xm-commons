package com.icthh.xm.commons.metric;

import static java.lang.Boolean.TRUE;
import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import io.github.jhipster.config.JHipsterProperties;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableMetrics(proxyTargetClass = true)
public class MetricsConfiguration extends MetricsConfigurerAdapter {

    private static final String PROP_METRIC_REG_JVM_MEMORY = "jvm.memory";
    private static final String PROP_METRIC_REG_JVM_GARBAGE = "jvm.garbage";
    private static final String PROP_METRIC_REG_JVM_THREADS = "jvm.threads";
    private static final String PROP_METRIC_REG_JVM_FILES = "jvm.files";
    private static final String PROP_METRIC_REG_JVM_BUFFERS = "jvm.buffers";
    private static final String PROP_METRIC_REG_JVM_ATTRIBUTE_SET = "jvm.attributes";
    private static final String PROP_METRIC_REG_OS = "os.attributes";
    private static final String PROP_METRIC_CONNECTION_TO_TOPIC = "kafka";

    private final Logger log = LoggerFactory.getLogger(MetricsConfiguration.class);

    private MetricRegistry metricRegistry = new MetricRegistry();

    private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    private final JHipsterProperties jhipsterProperties;
    private final KafkaAdmin kafkaAdmin;
    private final CollectorRegistry collectorRegistry;


    @Value("${spring.jmx.enabled:false}")
    private Boolean jmxEnabled;
    @Value("${application.kafkaMetric.enabled:false}")
    private Boolean kafkaMetricEnabled;
    @Value("${application.kafkaMetric.connectionTimeoutTopic:#{null}}")
    private Integer connectionTimeoutTopic;
    @Value("${application.kafkaMetric.metricTopics:#{null}}")
    private List<String> metricTopics;

    @Value("${management.metrics.export.prometheus.enabled}")
    private Boolean prometheusExportEnabled;

    public MetricsConfiguration(JHipsterProperties jhipsterProperties, KafkaAdmin kafkaAdmin,
          CollectorRegistry collectorRegistry) {
        this.jhipsterProperties = jhipsterProperties;
        this.kafkaAdmin = kafkaAdmin;
        this.collectorRegistry = collectorRegistry;
    }

    @Override
    @Bean
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @Override
    @Bean
    public HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }

    /**
     * Init commons metrics
     */
    @PostConstruct
    public void init() {
        log.debug("Registering JVM gauges");
        metricRegistry.register(PROP_METRIC_REG_JVM_MEMORY, new MemoryUsageGaugeSet());
        metricRegistry.register(PROP_METRIC_REG_JVM_GARBAGE, new GarbageCollectorMetricSet());
        metricRegistry.register(PROP_METRIC_REG_JVM_THREADS, new ThreadStatesGaugeSet());
        metricRegistry.register(PROP_METRIC_REG_JVM_FILES, new FileDescriptorRatioGauge());
        metricRegistry.register(PROP_METRIC_REG_JVM_BUFFERS, new BufferPoolMetricSet(getPlatformMBeanServer()));
        metricRegistry.register(PROP_METRIC_REG_JVM_ATTRIBUTE_SET, new JvmAttributeGaugeSet());
        metricRegistry.register(PROP_METRIC_REG_OS, new OperatingSystemGaugeSet(getOperatingSystemMXBean()));

        if (jmxEnabled) {
            log.debug("Initializing Metrics JMX reporting");
            JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
            jmxReporter.start();
        }
        if (jhipsterProperties.getMetrics().getLogs().isEnabled()) {
            log.info("Initializing Metrics Log reporting");
            Marker metricsMarker = MarkerFactory.getMarker("metrics");
            final Slf4jReporter reporter = Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(LoggerFactory.getLogger("metrics"))
                .markWith(metricsMarker)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
            reporter.start(jhipsterProperties.getMetrics().getLogs().getReportFrequency(), TimeUnit.SECONDS);
        }

        if (TRUE.equals(kafkaMetricEnabled)) {
            metricRegistry.register(PROP_METRIC_CONNECTION_TO_TOPIC,
                new KafkaMetricsSet(kafkaAdmin, connectionTimeoutTopic, metricTopics));
        }

        if (TRUE.equals(prometheusExportEnabled)) {
            collectorRegistry.register(new DropwizardExports(metricRegistry));
        }
    }
}
