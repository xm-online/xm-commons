package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.config.event.InitSourceEvent;
import com.icthh.xm.commons.domainevent.outbox.config.OutboxInitSourceEventListener;
import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OutboxInitSourceEventListenerTest {

    private static final String OUTBOX_TRANSPORT_BEAN_NAME = "outboxTransport";
    private static final String TEST_TENANT_KEY = "tenantKey";
    private OutboxInitSourceEventListener outboxInitSourceEventListener;

    @Mock
    private LiquibaseRunner liquibaseRunner;

    @Mock
    private ApplicationContext applicationContext;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(applicationContext.getBeanNamesForType(any(Class.class)))
            .thenReturn(new String[]{OUTBOX_TRANSPORT_BEAN_NAME});
        this.outboxInitSourceEventListener = new OutboxInitSourceEventListener(applicationContext, liquibaseRunner);
    }

    @Test
    public void shouldNotInitWrongTransport() {
        InitSourceEvent wrongEvent = new InitSourceEvent(new Object(), TEST_TENANT_KEY, "wrongTransport");
        outboxInitSourceEventListener.onApplicationEvent(wrongEvent);
        verify(liquibaseRunner, never()).runOnTenant(any(), any());
    }

    @Test
    public void shouldInitCorrectTransport() {
        InitSourceEvent event = new InitSourceEvent(new Object(), TEST_TENANT_KEY, OUTBOX_TRANSPORT_BEAN_NAME);
        outboxInitSourceEventListener.onApplicationEvent(event);
        verify(liquibaseRunner, times(1))
            .runOnTenant(eq(TEST_TENANT_KEY), any());
    }
}
