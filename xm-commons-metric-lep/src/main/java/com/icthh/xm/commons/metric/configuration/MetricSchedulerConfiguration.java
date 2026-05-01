package com.icthh.xm.commons.metric.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class MetricSchedulerConfiguration {

    @Bean(name = "periodicMetricsTaskScheduler")
    public ThreadPoolTaskScheduler periodicMetricsTaskScheduler(
            @Value("${application.periodicMetricPoolSize:5}") int poolSize) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(poolSize);
        taskScheduler.setThreadNamePrefix("pmetrics-executor-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(false);
        taskScheduler.setAwaitTerminationSeconds(1);
        taskScheduler.initialize();
        return taskScheduler;
    }
}
