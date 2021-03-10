package com.icthh.xm.commons.timeline.config;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.timeline.TimelineEventProducer;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TimelineInterceptorUnitTest {

    private TimelineInterceptor timelineInterceptor;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private TimelineEventProducer eventProducer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        timelineInterceptor = new TimelineInterceptor(eventProducer, Collections.emptyList(), asList("GET", "POST"));
    }

    @Test
    public void shouldNotRunLogicForGETMethod() {
        when(httpServletRequest.getMethod()).thenReturn("GET");
        timelineInterceptor.afterCompletion(httpServletRequest, null, null, null);

        verify(eventProducer, times(0)).createEventJson(any(), any(), any(), any(), any());
        verify(eventProducer, times(0)).send(any(), any());
    }

    @Test
    public void shouldNotRunLogicForPOSTMethod() {
        when(httpServletRequest.getMethod()).thenReturn("POST");
        timelineInterceptor.afterCompletion(httpServletRequest, null, null, null);

        verify(eventProducer, times(0)).createEventJson(any(), any(), any(), any(), any());
        verify(eventProducer, times(0)).send(any(), any());
    }

    @Test
    public void shouldRunLogicForPUTMethod() {
        when(httpServletRequest.getMethod()).thenReturn("PUT");
        timelineInterceptor.afterCompletion(httpServletRequest, null, null, null);

        verify(eventProducer, times(1)).createEventJson(any(), any(), any(), any(), any());
        verify(eventProducer, times(1)).send(any(), any());
    }
}
