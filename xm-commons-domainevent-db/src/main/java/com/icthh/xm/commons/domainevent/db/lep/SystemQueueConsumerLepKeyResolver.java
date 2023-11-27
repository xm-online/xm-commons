package com.icthh.xm.commons.domainevent.db.lep;

import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The {@link SystemQueueConsumerLepKeyResolver} class.
 */
@Component
public class SystemQueueConsumerLepKeyResolver implements LepKeyResolver {

    private static final String PARAM_EVENT = "event";

    @Override
    public List<String> segments(LepMethod method) {
        SystemEvent event = method.getParameter(PARAM_EVENT, SystemEvent.class);
        return List.of(event.getEventType());
    }
}
