package com.icthh.xm.commons.scheduler.service;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
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

    }

}
