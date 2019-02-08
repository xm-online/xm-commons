package com.icthh.xm.commons.scheduler.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerEventServiceUnitTest {

    @InjectMocks
    private SchedulerEventService eventService;

    @Spy
    private AllHandler handlerAllTypes = new AllHandler();
    @Mock
    private SchedulerEventHandler handlerOtherType;
    @Mock
    private SchedulerEventHandler handlerSameType;

    @Spy
    private List<SchedulerEventHandler> handlers = new ArrayList<>();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        handlers.add(handlerAllTypes);
        handlers.add(handlerOtherType);
        handlers.add(handlerSameType);
    }


    @Test
    public void testHandle() {
        ScheduledEvent event = new ScheduledEvent();
        event.setTypeKey("TEST_T_K");
        when(handlerSameType.eventType()).thenReturn("TEST_T_K");
        when(handlerOtherType.eventType()).thenReturn("OTHER");
        when(handlerAllTypes.eventType()).thenCallRealMethod();
        eventService.processSchedulerEvent(event, "TEST");

        verify(handlerOtherType, times(2)).eventType();
        verifyNoMoreInteractions(handlerOtherType);
        verify(handlerAllTypes).onEvent(refEq(event), eq("TEST"));
        verify(handlerSameType).onEvent(refEq(event), eq("TEST"));

    }

    private class AllHandler implements SchedulerEventHandler {
        @Override
        public void onEvent(ScheduledEvent scheduledEvent, String tenantKey) {}
    }

}
