package com.icthh.xm.commons.lep.impl;

import static com.icthh.xm.commons.metric.service.BusinessMetricsService.METRIC_LEP_EXECUTION_COUNT;
import static com.icthh.xm.commons.metric.service.BusinessMetricsService.METRIC_LEP_EXECUTION_TIME;
import static com.icthh.xm.commons.metric.service.BusinessMetricsService.STATUS_ERROR;
import static com.icthh.xm.commons.metric.service.BusinessMetricsService.STATUS_SUCCESS;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.metric.service.BusinessMetricsService;
import com.icthh.xm.commons.metric.service.MetricsPercentileHistogramLep;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class LepEngineMetricsDelegate {

    private static final String TAG_TENANT = "tenant";
    private static final String TAG_LEP_KEY = "lepKey";
    private static final String TAG_ENGINE = "engine";
    private static final String TAG_STATUS = "status";

    private final MetricsPercentileHistogramLep metricsPercentileLep;
    private final BusinessMetricsService metricsService;
    private final TenantContextHolder tenantContextHolder;

    public Object recordLepExecutionMetrics(LepEngine engine, Object lepKey, String engineId, 
                                          LepExecutionCallback callback) {
        log.info("LepEngine.invoke started: lepKey={}, engineId={}", lepKey, engineId);

        Map<String, String> tags = extractTags(engine, lepKey);

        try {
            Object result = metricsPercentileLep.recordTimerWithPercentileHistogram(
                METRIC_LEP_EXECUTION_TIME,
                tags, makeExecute(callback)
            );

            metricsService.incrementCounter(METRIC_LEP_EXECUTION_COUNT, withStatus(tags, STATUS_SUCCESS));

            log.info("LepEngine.invoke finished: lepKey={}, engineId={}", lepKey, engineId);
            return result;

        } catch (Throwable e) {
            metricsService.incrementCounter(METRIC_LEP_EXECUTION_COUNT, withStatus(tags, STATUS_ERROR));

            log.error("LepEngine.invoke failed: lepKey={}, engineId={}", lepKey, engineId, e);
            throw e;
        }
    }


    private Map<String, String> extractTags(LepEngine engine, Object lepKey) {
        return Map.of(
            TAG_TENANT, tenantContextHolder.getTenantKey(),
            TAG_LEP_KEY, lepKey != null ? lepKey.toString() : "noLepKey",
            TAG_ENGINE, engine.getClass().getSimpleName()
                        .replace("$$EnhancerByCGLIB$$", "")
                        .replaceAll("\\d+$", "")
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

    private Supplier<Object> makeExecute(LepExecutionCallback execution) {
        return () -> {
            try {
                return execution.execute();
            } catch (Throwable e) {
                throw new BusinessException(e.getMessage());
            }
        };
    }
}
