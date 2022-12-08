package com.icthh.xm.commons.domainevent.service.builder.impl;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.domainevent.service.builder.impl.DefaultDomainEventBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.CREATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class DomainEventFactoryTest {

    private final static String MS_NAME = "entity";
    private final static String TENANT_KEY = "tenant_core";
    private final static String CLIENT_ID = "clientId";
    private final static String USER_KEY = "userKey";
    private final static String TX_ID = "txId";

    private DomainEventFactory domainEventFactory;

    @Mock
    private DefaultDomainEventBuilder defaultDomainEventBuilder;
    @Mock
    private DefaultDomainEventBuilder transactionalDomainEventBuilder;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(defaultDomainEventBuilder.getPrefilledBuilder()).thenReturn(
            DomainEvent
                .builder()
                .msName(MS_NAME)
                .tenant(TENANT_KEY)
                .eventDate(Instant.now())
                .clientId(CLIENT_ID)
                .userKey(USER_KEY)
        );
        when(transactionalDomainEventBuilder.getPrefilledBuilder()).thenReturn(
            DomainEvent
                .builder()
                .msName(MS_NAME)
                .tenant(TENANT_KEY)
                .eventDate(Instant.now())
                .clientId(CLIENT_ID)
                .userKey(USER_KEY)
                .txId(TX_ID)
        );
        this.domainEventFactory = new DomainEventFactory(defaultDomainEventBuilder, Optional.of(transactionalDomainEventBuilder));
    }

    @Test
    public void shouldBuildPrefilledEvent() {
        DomainEvent event = domainEventFactory
            .build();

        assertEquals(MS_NAME, event.getMsName());
        assertEquals(TENANT_KEY, event.getTenant());
        assertNotNull(event.getEventDate());
        assertEquals(CLIENT_ID, event.getClientId());
        assertEquals(USER_KEY, event.getUserKey());
    }

    @Test
    public void shouldBuildPrefilledEventWithTransaction() {
        DomainEvent event = domainEventFactory
            .withTransaction()
            .build();

        assertEquals(TX_ID, event.getTxId());

        assertEquals(MS_NAME, event.getMsName());
        assertEquals(TENANT_KEY, event.getTenant());
        assertNotNull(event.getEventDate());
        assertEquals(CLIENT_ID, event.getClientId());
        assertEquals(USER_KEY, event.getUserKey());
    }

    @Test
    public void shouldReturnPrefilledBuilder() {
        DomainEvent event = domainEventFactory
            .builder()
            .aggregateType("aggregateType")
            .build();

        assertEquals(MS_NAME, event.getMsName());
        assertEquals(TENANT_KEY, event.getTenant());
        assertNotNull(event.getEventDate());
        assertEquals(CLIENT_ID, event.getClientId());
        assertEquals(USER_KEY, event.getUserKey());

        assertEquals("aggregateType", event.getAggregateType());
    }

    @Test
    public void shouldReturnPrefilledBuilderWithTransactionId() {
        DomainEvent event = domainEventFactory
            .withTransaction()
            .builder()
            .aggregateType("aggregateType")
            .build();

        assertEquals(TX_ID, event.getTxId());

        assertEquals(MS_NAME, event.getMsName());
        assertEquals(TENANT_KEY, event.getTenant());
        assertNotNull(event.getEventDate());
        assertEquals(CLIENT_ID, event.getClientId());
        assertEquals(USER_KEY, event.getUserKey());

        assertEquals("aggregateType", event.getAggregateType());
    }


    @Test
    public void shouldReturnBuiltObject() {
        DomainEvent event = domainEventFactory
            .build(CREATE, "aggregateId", "aggregateType");

        assertEquals(MS_NAME, event.getMsName());
        assertEquals(TENANT_KEY, event.getTenant());
        assertNotNull(event.getEventDate());
        assertEquals(CLIENT_ID, event.getClientId());
        assertEquals(USER_KEY, event.getUserKey());

        assertEquals("aggregateType", event.getAggregateType());
        assertEquals("aggregateId", event.getAggregateId());
        assertEquals(CREATE.name(), event.getOperation());
    }

    @Test
    public void shouldReturnBuiltEventWithTransaction() {
        DomainEvent event = domainEventFactory
            .withTransaction()
            .build(CREATE, "aggregateId", "aggregateType");

        assertEquals(TX_ID, event.getTxId());

        assertEquals(MS_NAME, event.getMsName());
        assertEquals(TENANT_KEY, event.getTenant());
        assertNotNull(event.getEventDate());
        assertEquals(CLIENT_ID, event.getClientId());
        assertEquals(USER_KEY, event.getUserKey());

        assertEquals("aggregateType", event.getAggregateType());
        assertEquals("aggregateId", event.getAggregateId());
        assertEquals(CREATE.name(), event.getOperation());
    }
}
