package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.filter.WebApiDomainEventFactory;
import com.icthh.xm.commons.domainevent.service.filter.WebFilterEngine;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiMaskConfig.class)
@EnableConfigurationProperties
public class WebApiSourceUnitTest {

    @Autowired
    private ApiMaskConfig apiMaskConfig;

    @Mock
    private XmDomainEventConfiguration xmDomainEventConfiguration;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Mock
    private WebFilterEngine webFilterEngine;

    @Mock
    private WebApiDomainEventFactory webApiDomainEventFactory;

    @Mock
    private ContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

    private WebApiSource webApiSource;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        webApiSource = new WebApiSource(eventPublisher, xmAuthenticationContextHolder, apiMaskConfig, xmDomainEventConfiguration, webFilterEngine);
    }


    @Test
    public void shouldPublishEvent_isIgnoreRequestReturnEvent() {
        DomainEvent domainEvent = DomainEvent.builder()
            .id(UUID.randomUUID())
            .build();
        when(webApiDomainEventFactory.createEvent(eq(request), eq(response), any(), any(), any(), any(), any())).thenReturn(domainEvent);
        when(webFilterEngine.isIgnoreRequest(eq(request), eq(response), any(), any())).thenReturn(domainEvent);

        webApiSource.afterCompletion(request, response, null, null);

        verify(eventPublisher, times(1)).publish(DefaultDomainEventSource.WEB.getCode(), domainEvent);
    }

    @Test
    public void shouldPublishEvent_isIgnoreRequestReturnNull() {
        when(webApiDomainEventFactory.createEvent(eq(request), eq(response), any(), any(), any(), any(), any())).thenReturn(null);
        when(webFilterEngine.isIgnoreRequest(eq(request), eq(response), any(), any())).thenReturn(null);

        webApiSource.afterCompletion(request, response, null, null);

        verify(eventPublisher, times(0)).publish(DefaultDomainEventSource.WEB.getCode(), null);
    }
}
