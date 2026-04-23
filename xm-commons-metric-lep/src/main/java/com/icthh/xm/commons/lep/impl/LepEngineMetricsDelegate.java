package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.metric.service.BusinessMetricsService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LepEngineMetricsDelegate {

    private static final String TAG_TENANT = "tenant";
    private static final String TAG_LEP_KEY = "lepKey";
    private static final String TAG_ENGINE = "engine";
    private static final String TAG_STATUS = "status";

    private final BusinessMetricsService metricsService;
    private final TenantContextHolder tenantContextHolder;

    public Object recordLepExecutionMetrics(LepEngine engine, Object lepKey, String engineId, 
                                          LepExecutionCallback callback) throws Throwable {
        log.info("LepEngine.invoke started: lepKey={}, engineId={}", lepKey, engineId);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            Object result = callback.execute();
            stopWatch.stop();
            
            recordMetrics(engine, lepKey, stopWatch.getTotalTimeMillis(), BusinessMetricsService.STATUS_SUCCESS);
            
            log.info("LepEngine.invoke finished: lepKey={}, engineId={}, tookMs={}",
                lepKey, engineId, stopWatch.getTotalTimeMillis());
            return result;
            
        } catch (Throwable e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            
            recordMetrics(engine, lepKey, stopWatch.getTotalTimeMillis(), BusinessMetricsService.STATUS_ERROR);
            
            log.error("LepEngine.invoke failed: lepKey={}, engineId={}, tookMs={}",
                lepKey, engineId, stopWatch.getTotalTimeMillis(), e);
            throw e;
        }
    }

    private void recordMetrics(LepEngine engine, Object lepKey, double executionTimeMs, String status) {
        Map<String, String> tags = extractTags(engine, lepKey);
        
        metricsService.recordDistribution(BusinessMetricsService.METRIC_LEP_EXECUTION_TIME, tags, executionTimeMs);
        metricsService.incrementCounter(BusinessMetricsService.METRIC_LEP_EXECUTION_COUNT, withStatus(tags, status));
    }

    private Map<String, String> extractTags(LepEngine engine, Object lepKey) {
        return Map.of(
            TAG_TENANT, tenantContextHolder.getTenantKey(),
            TAG_LEP_KEY, lepKey != null ? lepKey.toString() : "unknown",
            TAG_ENGINE, engine.getClass().getSimpleName()
        );
    }

    private Map<String, String> withStatus(Map<String, String> tags, String status) {
        return Map.of(
            TAG_TENANT, tags.get(TAG_TENANT),
            TAG_LEP_KEY, tags.get(TAG_LEP_KEY),
            TAG_ENGINE, tags.get(TAG_ENGINE),
            TAG_STATUS, status
        );
    }

    @FunctionalInterface
    public interface LepExecutionCallback {
        Object execute() throws Throwable;
    }
}
