package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepProcessingEvent.AfterExecutionEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.AfterProcessingEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeExecutionEvent;
import com.icthh.xm.lep.api.LepProcessingEvent.BeforeProcessingEvent;

/**
 * The {@link LepProcessingListenerAggregator} class.
 */
public interface LepProcessingListenerAggregator {

    void onBeforeProcessingEvent(BeforeProcessingEvent event);

    void onBeforeExecutionEvent(BeforeExecutionEvent event);

    void onAfterExecutionEvent(AfterExecutionEvent event);

    void onAfterProcessingEvent(AfterProcessingEvent event) ;

}
