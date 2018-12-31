package com.icthh.xm.commons.scheduler.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SchedulerEventService.class})
public class SchedulerEventServiceTest {

    @Autowired
    private SchedulerEventService eventService;

    @MockBean
    private SchedulerEventHandler handlerAllTypes;
    @MockBean
    private SchedulerEventHandler handlerOtherType;
    @MockBean
    private SchedulerEventHandler handlerSameType;

    @Test
    public void testHandle() {
        ScheduledEvent event = new ScheduledEvent();
        event.setTypeKey("TEST_T_K");
        when(handlerSameType.eventType()).thenReturn("TEST_T_K");
        when(handlerSameType.eventType()).thenReturn("OTHER");
        eventService.processSchedulerEvent(event, "TEST");

        verifyNoMoreInteractions(handlerOtherType);
        verify(handlerAllTypes).onEvent(refEq(event), eq("TEST"));
        verify(handlerSameType).onEvent(refEq(event), eq("TEST"));

    }

}
