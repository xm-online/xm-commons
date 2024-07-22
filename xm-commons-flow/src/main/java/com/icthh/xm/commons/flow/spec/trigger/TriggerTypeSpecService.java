package com.icthh.xm.commons.flow.spec.trigger;

import com.icthh.xm.commons.config.client.api.refreshable.MapRefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TriggerTypeSpecService extends MapRefreshableConfiguration<TriggerType, TriggerTypesSpec> {

    public TriggerTypeSpecService(@Value("${spring.application.name}") String appName,
                                  TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    protected List<TriggerType> toConfigItems(TriggerTypesSpec config) {
        return config.getTriggerTypes();
    }

    @Override
    public Class<TriggerTypesSpec> configFileClass() {
        return TriggerTypesSpec.class;
    }

    @Override
    public String configName() {
        return "trigger-types";
    }

    @Override
    public String folder() {
        return "/flow";
    }

    public List<TriggerType> triggerTypes() {
        return List.copyOf(getConfiguration().values());
    }

    public TriggerType getTrigger(String triggerType) {
        return getConfiguration().get(triggerType);
    }
}
