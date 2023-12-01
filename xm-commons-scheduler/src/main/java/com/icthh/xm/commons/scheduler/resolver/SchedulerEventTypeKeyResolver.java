package com.icthh.xm.commons.scheduler.resolver;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulerEventTypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("scheduledEvent", ScheduledEvent.class).getTypeKey());
    }
}
