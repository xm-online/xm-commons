package com.icthh.xm.commons.domainevent.config.event;


import com.icthh.xm.commons.domainevent.config.SourceConfig;
import com.icthh.xm.commons.domainevent.config.event.InitSourceEvent;
import com.icthh.xm.commons.domainevent.config.event.InitSourceEventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InitSourceEventPublisherTest {
    private final static String TEST_TENANT_KEY = "tenantKey";

    private InitSourceEventPublisher initSourceEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        this.initSourceEventPublisher = new InitSourceEventPublisher(applicationEventPublisher);
    }

    @Test
    public void shouldPublishDistinctTransport() {
        String transport1 = "transport1";
        String transport2 = "transport2";
        List<SourceConfig> sources = List.of(
            buildSourceConfig(true, transport1),
            buildSourceConfig(true, transport2),
            buildSourceConfig(true, transport1),
            buildSourceConfig(true, transport2)
        );
        initSourceEventPublisher.publish(TEST_TENANT_KEY, sources);
        verify(applicationEventPublisher, times(2)).publishEvent(any());
    }

    @Test
    public void shouldPublishEnabledTransport() {
        List<SourceConfig> sources = List.of(
            buildSourceConfig(true, "transport1"),
            buildSourceConfig(false, "transport2"),
            buildSourceConfig(false, "transport3")
        );
        initSourceEventPublisher.publish(TEST_TENANT_KEY, sources);
        verify(applicationEventPublisher, times(1)).publishEvent(any());
    }

    @Test
    public void shouldPublishTransport() {
        SourceConfig requestSourceConfig = buildSourceConfig(true, "transport1");
        List<SourceConfig> sources = List.of(
            requestSourceConfig
        );
        initSourceEventPublisher.publish(TEST_TENANT_KEY, sources);
        ArgumentCaptor<InitSourceEvent> eventCaptor = ArgumentCaptor.forClass(InitSourceEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        InitSourceEvent publishedEvent = eventCaptor.getValue();
        assertEquals(requestSourceConfig.getTransport(), publishedEvent.getTransport());
        assertEquals(TEST_TENANT_KEY, publishedEvent.getTenantKey());
    }

    private SourceConfig buildSourceConfig(boolean enabled, String transport) {
        SourceConfig sourceConfig = new SourceConfig();
        sourceConfig.setEnabled(enabled);
        sourceConfig.setTransport(transport);
        return sourceConfig;
    }

}
