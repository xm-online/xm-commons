package com.icthh.xm.commons.domainevent.db.lep;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

/**
 * The {@link SystemQueueConsumerLepKeyResolver} class.
 */
@Component
public class SystemQueueConsumerLepKeyResolver extends AppendLepKeyResolver {

    /**
     * Method parameter name for {@code SystemEvent}.
     */
    private static final String PARAM_EVENT = "event";

    /**
     * LEP extension key specification:<br>
     * {@code ChangeState$$<xm-entity-type-key>$$<next-state-key>}
     * <p>
     * Add to method name {@code <xm-entity-type-key} and {@code <state-key>} to the end.
     *
     * @param baseKey        base LEP key (prefix), can be {@code null}
     * @param method         method data on what LEP call occurs
     * @param managerService LEP manager service
     * @return dynamic value of LEP base key
     */
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        SystemEvent event = getRequiredParam(method, PARAM_EVENT, SystemEvent.class);
        String translatedEventType = translateToLepConvention(event.getEventType());

        return new String[]{
            translatedEventType
        };
    }

}
