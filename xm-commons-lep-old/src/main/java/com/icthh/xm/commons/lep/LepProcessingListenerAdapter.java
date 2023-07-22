package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.AfterExecutionEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.AfterProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeExecutionEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingListener;

/**
 * An implementation of {@link LepProcessingListener} with empty methods allowing
 * subclasses to override only the methods they're interested in.
 */
public abstract class LepProcessingListenerAdapter implements LepProcessingListenerAggregator {

    /**
     * Process event unknown for {@link LepProcessingListenerAdapter}.
     *
     * @param event the event
     */
    protected void onOtherEvent(LepProcessingEvent event) {
        throw new IllegalStateException("Unsupported " + LepProcessingEvent.class.getSimpleName() + " subtype: "
                                            + event.getClass().getCanonicalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBeforeProcessingEvent(BeforeProcessingEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBeforeExecutionEvent(BeforeExecutionEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAfterExecutionEvent(AfterExecutionEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAfterProcessingEvent(AfterProcessingEvent event) {

    }

}
