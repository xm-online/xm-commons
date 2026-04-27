package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessMetricsService implements LepAdditionalContext<BusinessMetricsService> {

    private static final String METRIC_PREFIX = "business.metrics.";

    public static final String METRIC_LEP_EXECUTION_TIME = "lep.execution.time";
    public static final String METRIC_LEP_EXECUTION_COUNT = "lep.execution.count";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    private final MeterRegistry meterRegistry;
    private final TenantContextHolder tenantContextHolder;

    public void incrementCounter(String name, Map<String, String> tags) {
        meterRegistry.counter(toTenantMetricName(name), MetricsTagsUtil.toTags(tags)).increment();
    }

    public void incrementCounter(String name, Map<String, String> tags, double amount) {
        meterRegistry.counter(toTenantMetricName(name), toTags(tags)).increment(amount);
    }

    public void recordTimer(String name, Map<String, String> tags, long duration, TimeUnit unit) {
        meterRegistry.timer(toTenantMetricName(name), MetricsTagsUtil.toTags(tags)).record(duration, unit);
    }

    public <T> T recordTimer(String name, Map<String, String> tags, Supplier<T> action) {
        return meterRegistry.timer(toTenantMetricName(name), MetricsTagsUtil.toTags(tags)).record(action);
    }

    public void recordDistribution(String name, Map<String, String> tags, double value) {
        meterRegistry.summary(toTenantMetricName(name), toTags(tags)).record(value);
    }

    private String toTenantMetricName(String name) {
        return String.format("%s.%s.%s",
            METRIC_PREFIX,
            tenantContextHolder.getTenantKey(),
            name);
    }

    private Tags toTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Tags.empty();
        }
        return Tags.of(tags.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), e.getValue()))
            .toList());
    }

    @Override
    @IgnoreLogginAspect
    public String additionalContextKey() {
        return BusinessMetricsServiceField.FIELD_NAME;
    }

    @Override
    @IgnoreLogginAspect
    public BusinessMetricsService additionalContextValue() {
        return this;
    }

    @Override
    public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
        return BusinessMetricsServiceField.class;
    }

    public interface BusinessMetricsServiceField extends LepAdditionalContextField {
        String FIELD_NAME = "businessMetricsService";
        default BusinessMetricsService getBusinessMetricsService() {
            return (BusinessMetricsService)get(FIELD_NAME);
        }
    }
}
