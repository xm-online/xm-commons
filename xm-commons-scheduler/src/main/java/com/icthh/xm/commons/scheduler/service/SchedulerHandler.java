package com.icthh.xm.commons.scheduler.service;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.metric.SchedulerMetricsSet;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerHandler implements SchedulerEventHandler {

    private final SchedulerService schedulerService;
    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;
    private final SchedulerMetricsSet schedulerMetricsSet;

    @Override
    public void onEvent(ScheduledEvent scheduledEvent, String tenant) {
        tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenant), () -> {
            try (var context = lepManagementService.beginThreadContext()) {
                log.info("Receive event {} {}", scheduledEvent, tenant);
                schedulerService.onEvent(scheduledEvent);
                schedulerMetricsSet.onSuccess();
            } catch (Throwable e) {
                schedulerMetricsSet.onError();
                throw e;
            }
        });
    }
}
