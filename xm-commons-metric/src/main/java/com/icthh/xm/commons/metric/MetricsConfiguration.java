package com.icthh.xm.commons.metric;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ KafkaMetricsConfiguration.class })
public class MetricsConfiguration {

    @Bean
    public MeterBinder operatingSystemMetricsBinder() {
        return new OperatingSystemMetrics(getOperatingSystemMXBean());
    }

    @Bean
    public TenantClientMetricObserver tenantClientMetricObserver(MeterRegistry meterRegistry) {
        return new TenantClientMetricObserver(meterRegistry);
    }
}
