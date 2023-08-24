package com.icthh.xm.commons.scheduler.service;

import com.icthh.xm.commons.lep.api.LepEngineSession;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.metric.SchedulerMetricsSet;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerHandler implements SchedulerEventHandler {

    private final SchedulerService schedulerService;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;
    private final LepManagementService lepManager;
    private final SchedulerMetricsSet schedulerMetricsSet;

    @Override
    @SneakyThrows
    public void onEvent(ScheduledEvent scheduledEvent, String tenant) {
        try (var threadContext = init(tenant)) {
            log.info("Receive event {} {}", scheduledEvent, tenant);
            schedulerService.onEvent(scheduledEvent);
            schedulerMetricsSet.onSuccess();
        } catch (Throwable e) {
            schedulerMetricsSet.onError();
            throw e;
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        }
    }

    private LepEngineSession init(String tenant) {
        TenantContextUtils.setTenant(tenantContextHolder, tenant);
        return lepManager.beginThreadContext();
    }
}
