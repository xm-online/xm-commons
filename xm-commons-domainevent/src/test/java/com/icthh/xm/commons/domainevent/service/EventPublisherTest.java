package com.icthh.xm.commons.domainevent.service;

import com.icthh.xm.commons.domainevent.config.DbSourceConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventPublisherTest {

    private static final String DB_SOURCE = "DB";
    private static final String TENANT_KEY = "TEST";

    private EventPublisher eventPublisher;

    @Mock
    private XmDomainEventConfiguration xmDomainEventConfiguration;
    @Mock
    private Transport testTransport;
    @Mock
    private TenantContextHolder tenantContextHolder;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        DbSourceConfig sourceConfig = new DbSourceConfig();
        sourceConfig.setEnabled(true);
        sourceConfig.setTransport("outboxTransport");
        when(tenantContextHolder.getTenantKey()).thenReturn(TENANT_KEY);
        when(xmDomainEventConfiguration.getTransport(eq(TENANT_KEY), eq(DB_SOURCE))).thenReturn(testTransport);
        this.eventPublisher = new EventPublisher(xmDomainEventConfiguration, tenantContextHolder);
    }

    @Test
    public void shouldPublishDomainEvent() {
        DomainEvent domainEvent = new DomainEvent();
        domainEvent.setId(UUID.randomUUID());
        eventPublisher.publish(DB_SOURCE, domainEvent);
        verify(testTransport).send(domainEvent);
    }

    @Test
    public void shouldThrowIfNotConfigured() {
        DomainEvent domainEvent = new DomainEvent();
        domainEvent.setId(UUID.randomUUID());
        Exception exception = null;
        try {
            eventPublisher.publish("LEP", domainEvent);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(NullPointerException.class, exception.getClass());
    }
}
