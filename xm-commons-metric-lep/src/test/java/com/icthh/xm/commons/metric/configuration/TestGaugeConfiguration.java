package com.icthh.xm.commons.metric.configuration;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.metric.resolver.GaugeMetricKeyResolver;
import com.icthh.xm.commons.metric.service.CustomGaugeService;
import com.icthh.xm.commons.metric.service.PeriodGaugeMetricsService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TestGaugeConfiguration {

    public TestGaugeConfiguration() { }

    @Bean
    public CustomGaugeConfiguration customGaugeConfiguration(MeterRegistry meterRegistry,
                                                             CustomGaugeService customGaugeService,
                                                             PeriodGaugeMetricsService periodGaugeMetricsService,
                                                             ApplicationNameProvider applicationNameProvider) {
        return new CustomGaugeConfiguration(meterRegistry, customGaugeService, periodGaugeMetricsService, applicationNameProvider.getAppName());
    }

    @Bean
    public CustomGaugeService customGaugeService(TenantContextHolder tenantContextHolder, LepManagementService lepManagementService) {
        return new CustomGaugeService(tenantContextHolder, lepManagementService);
    }

    @Bean
    public PeriodGaugeMetricsService periodGaugeMetricsService(TaskScheduler taskScheduler, CustomGaugeService customGaugeService) {
        return new PeriodGaugeMetricsService(taskScheduler, customGaugeService);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public GaugeMetricKeyResolver gaugeMetricKeyResolver() {
        return new GaugeMetricKeyResolver();
    }
}
