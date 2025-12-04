package com.icthh.xm.commons.metric;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;

import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    KafkaMetricsConfiguration.class
})
@EnableMetrics(proxyTargetClass = true)
public class MetricsConfiguration extends MetricsConfigurerAdapter {

    @Bean
    public MeterBinder operatingSystemMetricsBinder() {
        return new OperatingSystemMetrics(getOperatingSystemMXBean());
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
