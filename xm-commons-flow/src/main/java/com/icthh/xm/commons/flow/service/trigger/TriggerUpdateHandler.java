package com.icthh.xm.commons.flow.service.trigger;

import com.icthh.xm.commons.flow.domain.dto.Trigger;
import com.icthh.xm.commons.flow.service.YamlConverter;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TriggerUpdateHandler {

    private final TriggerProcessor triggerProcessor;
    private final YamlConverter yamlConverter;
    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagement;

    public void processTriggerUpdate(String tenantKey, String triggerYml) {
        Trigger trigger = yamlConverter.readConfig(triggerYml, Trigger.class);
        tenantContextHolder.getPrivilegedContext().execute(tenantKey, () -> {
            try (var context = lepManagement.beginThreadContext()) {
                triggerProcessor.processTriggerUpdate(tenantKey, trigger);
            }
        });
    }

}
