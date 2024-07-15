package com.icthh.xm.commons.flow.service.trigger;

import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.domain.Trigger;
import com.icthh.xm.commons.flow.service.resolver.TriggerResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@LepService(group = "flow.trigger")
public class TriggerProcessor {
    @LogicExtensionPoint(value = "TriggerUpdated", resolver = TriggerResolver.class)
    public List<Configuration> processTriggerUpdate(Trigger trigger, Flow flow) {
        return List.of();
    }

    @LogicExtensionPoint(value = "TriggerDeleted", resolver = TriggerResolver.class)
    public List<Configuration> processTriggerDelete(Trigger trigger, Flow flow) {
        return List.of();
    }
}
