package com.icthh.xm.commons.metric;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;

import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableMetrics(proxyTargetClass = true)
public class MetricsConfiguration extends MetricsConfigurerAdapter {

    private final Logger log = LoggerFactory.getLogger(MetricsConfiguration.class);

    private MeterRegistry meterRegistry;

    private KafkaAdmin kafkaAdmin;

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

    @Bean
    public MeterBinder operatingSystemMetricsBinder() {
        return new OperatingSystemMetrics(getOperatingSystemMXBean());
    }

    @Bean
    public MeterBinder kafkaMetricsBinder() {
        return new KafkaMetrics(kafkaAdmin, connectionTimeoutTopic, metricTopics);
    }

    @Bean
    public InitializingBean metricsInitializer(
        MeterRegistry meterRegistry,
        List<MeterBinder> meterBinders) {
        return () -> {
            for (MeterBinder binder : meterBinders) {
                binder.bindTo(meterRegistry);
            }
        };
    }
}
