package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.lep.api.LepProcessingEvent;
import org.springframework.context.ApplicationEvent;

/**
 * The {@link ApplicationLepProcessingEvent} class.
 */
public class ApplicationLepProcessingEvent extends ApplicationEvent {

    /**
     * Encapsulated LEP processing event.
     */
    private final LepProcessingEvent lepProcessingEvent;

    /**
     * Create a new ApplicationLepProcessingEvent.
     *
     * @param lepProcessingEvent lep processing event to resend (never {@code null})
     */
    public ApplicationLepProcessingEvent(LepProcessingEvent lepProcessingEvent) {
        super(lepProcessingEvent.getSource());
        this.lepProcessingEvent = lepProcessingEvent;
    }

    /**
     * Gets encapsulated LEP processing event.
     *
     * @return LEP processing event
     */
    public LepProcessingEvent getLepProcessingEvent() {
        return lepProcessingEvent;
    }

}
