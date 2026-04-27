package com.icthh.xm.commons.metric.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.metric.configuration.CustomGauge;
import com.icthh.xm.commons.metric.resolver.GaugeMetricKeyResolver;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static java.util.Collections.emptyMap;

@Slf4j
@Component
@IgnoreLogginAspect
@LepService(group = "gauges")
@RequiredArgsConstructor
public class CustomGaugeService {

    private final Map<String, Map<String, Object>> metricsCache = new ConcurrentHashMap<>();

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;
    private CustomGaugeService self;

    @Autowired
    public void setSelf(@Lazy CustomGaugeService self) {
        this.self = self;
    }

    public Object getMetric(String name, CustomGauge config, String tenantKey) {
        return runInTenantContext(tenantKey, () -> {
            if (config.getUpdatePeriodSeconds() == null) {
                return self.metricByName(name);
            }
            return metricsCache.getOrDefault(tenantKey, emptyMap()).get(name);
        });
    }

    public void updateMetric(String metricName, String tenant) {
        try {
            MdcUtils.putRid(MdcUtils.generateRid() + ":" + tenant);
            Object metricValue = runInTenantContext(tenant, () -> self.metricByName(metricName));
            metricsCache.computeIfAbsent(tenant, k -> new ConcurrentHashMap<>())
                .put(metricName, metricValue);
        } catch (Throwable e) {
            log.error("Error update metric", e);
        } finally {
            MdcUtils.clear();
        }
    }

    private Object runInTenantContext(String tenant, Supplier<Object> operation) {
        return tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant), () -> {
            try (var context = lepManagementService.beginThreadContext()) {
                return operation.get();
            }
        });
    }

    @LogicExtensionPoint(value = "Gauge", resolver = GaugeMetricKeyResolver.class)
    public Object metricByName(String metricName) {
        return self.metric(metricName);
    }

    @LogicExtensionPoint(value = "Gauge")
    public Object metric(String metricName) {
        log.warn("No lep for gauge metric {} found", metricName);
        return null;
    }
}
