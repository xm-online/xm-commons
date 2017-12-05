package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingListener;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Objects;

/**
 * The {@link ApplicationLepProcessingEventPublisher} class.
 */
public class ApplicationLepProcessingEventPublisher implements LepProcessingListener {

    private final ApplicationEventPublisher publisher;

    public ApplicationLepProcessingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(LepProcessingEvent lepProcessingEvent) {
        ApplicationLepProcessingEvent springAppEvent = new ApplicationLepProcessingEvent(lepProcessingEvent);
        publisher.publishEvent(springAppEvent);
    }

}
