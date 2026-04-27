package com.icthh.xm.commons.metric.configuration;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.metric.service.CustomGaugeService;
import com.icthh.xm.commons.metric.service.PeriodGaugeMetricsService;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class CustomGaugeConfiguration implements RefreshableConfiguration {

    private static final String METRICS_PREFIX = "custom.gauge.metrics";
    private static final String TENANT_NAME = "tenantName";

    private final ObjectMapper mapper = YamlMapperUtils.yamlDefaultMapper();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final MeterRegistry meterRegistry;
    private final CustomGaugeService customGaugeService;
    private final PeriodGaugeMetricsService periodGaugeMetricsService;
    private final String mappingPath;

    public CustomGaugeConfiguration(MeterRegistry meterRegistry,
                                    CustomGaugeService customGaugeService,
                                    PeriodGaugeMetricsService periodGaugeMetricsService,
                                    @Value("${spring.application.name}") String appName) {
        this.meterRegistry = meterRegistry;
        this.customGaugeService = customGaugeService;
        this.periodGaugeMetricsService = periodGaugeMetricsService;
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/metrics.yml";
    }

    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(mappingPath, updatedKey).get(TENANT_NAME);

            cleanupTenant(tenant);

            if (StringUtils.isBlank(config)) {
                return;
            }

            List<CustomGauge> metrics = mapper.readValue(config, new TypeReference<>() {});
            log.info("Gauge metric configuration updated for tenant [{}]: {} metrics", tenant, metrics.size());

            metrics.forEach(gauge -> registerGauge(gauge, tenant));

            periodGaugeMetricsService.scheduleGauges(metrics, tenant);
        } catch (Exception e) {
            log.error("Error reading metric configuration from {}", updatedKey, e);
        }
    }

    private void registerGauge(CustomGauge metric, String tenant) {
        String metricName = toTenantMetricName(metric.getName(), tenant);

        Gauge.builder(metricName,
                customGaugeService,
                svc -> toDouble(svc.getMetric(metric.getName(), metric, tenant)))
            .register(meterRegistry);
    }

    private void cleanupTenant(String tenant) {
        String tenantLower = tenant.toLowerCase();
        String metricPrefix = METRICS_PREFIX + "." + tenantLower;

        periodGaugeMetricsService.cancelGauges(tenantLower);

        meterRegistry.getMeters().stream()
            .filter(m -> m.getId().getName().startsWith(metricPrefix))
            .forEach(meterRegistry::remove);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(mappingPath, updatedKey);
    }

    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return 0d;
    }

    private String toTenantMetricName(String name, String tenant) {
        return String.format("%s.%s.%s",
            METRICS_PREFIX,
            tenant.toLowerCase(),
            name);
    }
}
