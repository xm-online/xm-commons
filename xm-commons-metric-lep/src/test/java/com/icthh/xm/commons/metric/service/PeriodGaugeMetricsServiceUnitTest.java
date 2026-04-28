package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.metric.configuration.CustomGauge;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PeriodGaugeMetricsServiceUnitTest {

    private static final String TENANT = "XM";
    private static final String TENANT_LOWER = "xm";

    private PeriodGaugeMetricsService periodGaugeMetricsService;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private CustomGaugeService customGaugeService;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @BeforeEach
    void init() {
        periodGaugeMetricsService = new PeriodGaugeMetricsService(taskScheduler, customGaugeService);
    }

    @Test
    void scheduleGaugesWithUpdatePeriod() {
        CustomGauge gauge = customGauge("orderCount", 30);

        doReturn(scheduledFuture)
            .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));

        periodGaugeMetricsService.scheduleGauges(List.of(gauge), TENANT);

        verify(taskScheduler).scheduleAtFixedRate(
            runnableCaptor.capture(),
            eq(Duration.ofSeconds(30))
        );
    }

    @Test
    void scheduleGaugesMultiple() {
        CustomGauge gauge1 = customGauge("orderCount", 30);
        CustomGauge gauge2 = customGauge("userCount", 60);

        doReturn(scheduledFuture)
            .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));

        periodGaugeMetricsService.scheduleGauges(List.of(gauge1, gauge2), TENANT);

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(30)));
        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(60)));
    }

    @Test
    void scheduleGaugesNotScheduleWhenUpdatePeriodIsNull() {
        CustomGauge gauge = customGauge("orderCount", null);

        periodGaugeMetricsService.scheduleGauges(List.of(gauge), TENANT);

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    void scheduleGaugesNotScheduleWhenUpdatePeriodIsZero() {
        CustomGauge gauge = customGauge("orderCount", 0);

        periodGaugeMetricsService.scheduleGauges(List.of(gauge), TENANT);

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    void scheduleGaugesNotScheduleWhenUpdatePeriodIsNegative() {
        CustomGauge gauge = customGauge("orderCount", -1);

        periodGaugeMetricsService.scheduleGauges(List.of(gauge), TENANT);

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    void scheduleGaugesCancelScheduledTasks() {
        CustomGauge gauge = customGauge("orderCount", 30);

        doReturn(scheduledFuture)
            .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));

        periodGaugeMetricsService.scheduleGauges(List.of(gauge), TENANT);
        periodGaugeMetricsService.cancelGauges(TENANT_LOWER);

        verify(scheduledFuture).cancel(eq(false));
    }

    @Test
    void scheduleGaugesCancelMultipleTasksForTenant() {
        doReturn(scheduledFuture)
            .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));

        CustomGauge gauge1 = customGauge("orderCount", 30);
        CustomGauge gauge2 = customGauge("userCount", 60);

        periodGaugeMetricsService.scheduleGauges(List.of(gauge1, gauge2), TENANT);
        periodGaugeMetricsService.cancelGauges(TENANT_LOWER);

        verify(scheduledFuture, times(2)).cancel(eq(false));
    }

    @Test
    void scheduleGaugesSkipNullPeriodInList() {
        CustomGauge withPeriod = customGauge("orderCount", 30);
        CustomGauge withoutPeriod = customGauge("realtimeMetric", null);

        doReturn(scheduledFuture)
            .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));

        periodGaugeMetricsService.scheduleGauges(List.of(withPeriod, withoutPeriod), TENANT);

        verify(taskScheduler).scheduleAtFixedRate(runnableCaptor.capture(), eq(Duration.ofSeconds(30)));
        runnableCaptor.getValue().run();

        verify(customGaugeService).updateMetric(eq("orderCount"), eq(TENANT));
        verify(customGaugeService, never()).updateMetric(eq("realtimeMetric"), eq(TENANT));
    }

    private CustomGauge customGauge(String name, Integer updatePeriodSeconds) {
        CustomGauge gauge = new CustomGauge();
        gauge.setName(name);
        gauge.setUpdatePeriodSeconds(updatePeriodSeconds);
        return gauge;
    }
}
