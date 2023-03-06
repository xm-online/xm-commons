package com.icthh.xm.commons.timeline.config;

import com.icthh.xm.commons.timeline.TimelineEventProducer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimelineInterceptorUnitTest {

    private static final String RESPONSE_TYPE_KEY_LEAD = "{\"typeKey\": \"LEAD\"}";
    private static final String RESPONSE_TYPE_KEY_FAKE_LEAD = "{\"typeKey\": \"FAKE-LEAD\"}";

    private TimelineInterceptor timelineInterceptor;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    ContentCachingResponseWrapper contentCachingResponseWrapper;

    @Mock
    private TimelineEventProducer eventProducer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        timelineInterceptor = new TimelineInterceptor(eventProducer, Collections.emptyList(), asList("GET", "POST"), List.of("LEAD"));
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
        byte[] bytes = RESPONSE_TYPE_KEY_FAKE_LEAD.getBytes();
        when(contentCachingResponseWrapper.getContentAsByteArray()).thenReturn(bytes);
        timelineInterceptor.afterCompletion(httpServletRequest, contentCachingResponseWrapper, null, null);

        verify(eventProducer, times(1)).createEventJson(any(), any(), any(), any(), any());
        verify(eventProducer, times(1)).send(any(), any());
    }

    @Test
    public void shouldNotRunLogicForLEADTypeKey() {
        byte[] bytes = RESPONSE_TYPE_KEY_LEAD.getBytes();
        when(contentCachingResponseWrapper.getContentAsByteArray()).thenReturn(bytes);
        timelineInterceptor.afterCompletion(httpServletRequest, contentCachingResponseWrapper, null, null);

        verify(eventProducer, times(0)).createEventJson(any(), any(), any(), any(), any());
        verify(eventProducer, times(0)).send(any(), any());
    }

    @Test
    public void shouldRunLogicForFakeTypeKey() {
        byte[] bytes = RESPONSE_TYPE_KEY_FAKE_LEAD.getBytes();
        when(contentCachingResponseWrapper.getContentAsByteArray()).thenReturn(bytes);
        timelineInterceptor.afterCompletion(httpServletRequest, contentCachingResponseWrapper, null, null);

        verify(eventProducer, times(1)).createEventJson(any(), any(), any(), any(), any());
        verify(eventProducer, times(1)).send(any(), any());
    }
}
