package com.icthh.xm.commons.domainevent.db.service;

import com.icthh.xm.commons.domainevent.config.SourceConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource.DB;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DatabaseSourceInterceptorUnitTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private XmDomainEventConfiguration xmDomainEventConfiguration;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private JpaEntityMapper jpaEntityMapper;

    @Mock
    private DatabaseFilter databaseFilter;

    @Mock
    private DatabaseDslFilter databaseDslFilter;

    private DatabaseSourceInterceptor databaseSourceInterceptor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        databaseSourceInterceptor = new DatabaseSourceInterceptor(
            entityManager, xmDomainEventConfiguration, eventPublisher, jpaEntityMapper, databaseFilter, databaseDslFilter);
    }

    @Test
    void onFlushDirty() {

        SourceConfig sourceConfig = new SourceConfig();
        doReturn(sourceConfig).when(xmDomainEventConfiguration).getSourceConfig(DB.name());

        databaseSourceInterceptor.onFlushDirty(null, null, null, null, null, null);

        verify(jpaEntityMapper, times(0)).map(any());
        verify(eventPublisher, times(0)).publish(any(), any());
        verify(databaseFilter, times(0)).lepFiltering(any(), any(), any());
        verify(databaseDslFilter, times(0)).lepFiltering(any(), any(), any());
        verify(entityManager, times(0)).getMetamodel();
    }

    @Test
    void onSave() {
    }

    @Test
    void onDelete() {
    }
}
