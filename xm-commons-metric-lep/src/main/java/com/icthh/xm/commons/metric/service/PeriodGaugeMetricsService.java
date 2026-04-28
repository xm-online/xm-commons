package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.metric.configuration.CustomGauge;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodGaugeMetricsService {

    private final TaskScheduler taskScheduler;
    private final CustomGaugeService customGaugeService;

    private final Map<String, Map<String, ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleGauges(List<CustomGauge> metrics, String tenant) {
        String tenantLower = tenant.toLowerCase();

        for (CustomGauge metric : metrics) {
            if (metric.getUpdatePeriodSeconds() != null && metric.getUpdatePeriodSeconds() > 0) {
                ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(
                    () -> customGaugeService.updateMetric(metric.getName(), tenant),
                    Duration.ofSeconds(metric.getUpdatePeriodSeconds())
                );
                scheduledTasks
                    .computeIfAbsent(tenantLower, k -> new ConcurrentHashMap<>())
                    .put(metric.getName(), task);
            }
        }
    }

    public void cancelGauges(String tenantLower) {
        Map<String, ScheduledFuture<?>> tasks = scheduledTasks.remove(tenantLower);
        if (tasks != null) {
            tasks.values().forEach(t -> t.cancel(false));
        }
    }
}
