package com.icthh.xm.commons.domainevent.db.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.domainevent.config.DbSourceConfig;
import com.icthh.xm.commons.domainevent.config.Filter;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.db.domain.Entity;
import com.icthh.xm.commons.domainevent.db.domain.EntityWithTableAnnotation;
import com.icthh.xm.commons.domainevent.db.domain.EntityWithoutTableAnnotation;
import com.icthh.xm.commons.domainevent.db.domain.MetamodelMock;
import com.icthh.xm.commons.domainevent.db.service.mapper.JpaEntityMapper;
import com.icthh.xm.commons.domainevent.db.service.mapper.impl.TypeKeyAwareJpaEntityMapper;
import com.icthh.xm.commons.domainevent.domain.DbDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventBuilder;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.CREATE;
import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.DELETE;
import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.UPDATE;
import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource.DB;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for DatabaseSourceInterceptor class.
 */
public class DatabaseSourceInterceptorUnitTest {

    private final static Serializable ID = 1L;
    private final static String TYPE_KEY_FIELD = "typeKey";
    private final static String NAME_FIELD = "name";
    private final static String STATE_KEY_FIELD = "stateKey";
    private final static String KEY_FIELD = "key";
    private final static String DESCRIPTION_FIELD = "description";
    private final static String TYPE_KEY = "LEAD";
    private final static String NAME = "NAME";
    private final static String NAME_NEW = "NAME_NEW";
    private final static String STATE = "STATE";
    private final static String STATE_NEW = "STATE_NEW";
    private final static String KEY = "SOME_KEY";
    private final static String DESCRIPTION = "DESCRIPTION";
    private final static String DESCRIPTION_NEW = "DESCRIPTION_NEW";
    private final static String TENANT_KEY = "RESTINTEST";

    @Mock
    private EntityManager entityManager;
    @Mock
    private XmDomainEventConfiguration xmDomainEventConfiguration;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    DomainEventBuilder domainEventBuilder;
    @Mock
    private DatabaseFilter databaseFilter;
    @Mock
    private DatabaseDslFilter databaseDslFilter;
    @Mock
    private TenantContextHolder tenantContextHolder;

    private JpaEntityMapper jpaEntityMapper;

    private ObjectMapper objectMapper;

    private DatabaseSourceInterceptor databaseSourceInterceptor;

    UUID uuid = UUID.randomUUID();

    MockedStatic<UUID> mocked;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        objectMapper = new ObjectMapper(new YAMLFactory());

        mocked = mockStatic(UUID.class);
        mocked.when(UUID::randomUUID).thenReturn(uuid);

        DomainEventFactory domainEventFactory = spy(new DomainEventFactory(domainEventBuilder, Optional.of(domainEventBuilder)));
        when(domainEventBuilder.getPrefilledBuilder()).thenReturn(DomainEvent.builder());
        jpaEntityMapper = spy(new TypeKeyAwareJpaEntityMapper(domainEventFactory));

        when(tenantContextHolder.getTenantKey()).thenReturn(TENANT_KEY);

        databaseSourceInterceptor = new DatabaseSourceInterceptor(entityManager, xmDomainEventConfiguration, eventPublisher,
            jpaEntityMapper, databaseFilter, databaseDslFilter, tenantContextHolder);
    }

    @After
    public void after() {
        mocked.close();
    }

    @Test
    public void onFlushDirty_notProcessConfig_withEnableFalse() {

        DbSourceConfig sourceConfig = buildDbSourceConfig(false, null);
        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());

        databaseSourceInterceptor.onFlushDirty(null, null, null, null, null, null);

        verify(jpaEntityMapper, times(0)).map(any());
        verify(eventPublisher, times(0)).publish(any(), any());
        verify(databaseFilter, times(0)).lepFiltering(any(), any(), any());
        verify(databaseDslFilter, times(0)).lepFiltering(any(), any(), any());
        verify(entityManager, times(0)).getMetamodel();
    }

    @Test
    public void onFlushDirty_shouldConfigTrueAndFilterNull() {

        DbSourceConfig sourceConfig = buildDbSourceConfig(true, null);
        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        Entity entity = buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, true);

        databaseSourceInterceptor.onFlushDirty(entity, ID, new Object[]{}, new Object[]{}, new String[]{}, null);

        verify(jpaEntityMapper, times(0)).map(any());
        verify(eventPublisher, times(0)).publish(any(), any());
        verify(databaseFilter, times(0)).lepFiltering(any(), any(), any());
        verify(databaseDslFilter, times(0)).lepFiltering(any(), any(), any());
        verify(entityManager, times(0)).getMetamodel();
    }

    @Test
    @SneakyThrows
    public void onFlushDirty_processEntityWithLepDatabaseFilterReturnTrue() {

        String config = readConfigFile("/dbSourceConfig.yml");
        DbSourceConfig sourceConfig = objectMapper.readValue(config, DbSourceConfig.class);

        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        doReturn(true).when(databaseFilter).lepFiltering(any(), any(), any());
        doReturn(null).when(databaseDslFilter).lepFiltering(any(), any(), any());

        databaseSourceInterceptor.onFlushDirty(
            buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, true),
            ID,
            new Object[]{TYPE_KEY, NAME_NEW, STATE_NEW, KEY, DESCRIPTION_NEW},
            new Object[]{TYPE_KEY, NAME, STATE, KEY, DESCRIPTION},
            new String[]{TYPE_KEY_FIELD, NAME_FIELD, STATE_KEY_FIELD, KEY_FIELD, DESCRIPTION_FIELD},
            null);

        DomainEvent expectedDbDomainEvent = buildDomainEvent(
            uuid,
            ID.toString(),
            TYPE_KEY,
            UPDATE,
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME, STATE_KEY_FIELD, STATE, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION),
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME_NEW, STATE_KEY_FIELD, STATE_NEW, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION_NEW)
        );

        verify(eventPublisher).publish(DB.name(), expectedDbDomainEvent);

    }

    @Test
    @SneakyThrows
    public void onFlushDirty_processEntityWithLepDatabaseDslFilterReturnTrue() {

        String config = readConfigFile("/dbSourceConfig.yml");
        DbSourceConfig sourceConfig = objectMapper.readValue(config, DbSourceConfig.class);

        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        doReturn(null).when(databaseFilter).lepFiltering(any(), any(), any());
        doReturn(true).when(databaseDslFilter).lepFiltering(any(), any(), any());

        databaseSourceInterceptor.onFlushDirty(
            buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, true),
            ID,
            new Object[]{TYPE_KEY, NAME_NEW, STATE_NEW, KEY, DESCRIPTION_NEW},
            new Object[]{TYPE_KEY, NAME, STATE, KEY, DESCRIPTION},
            new String[]{TYPE_KEY_FIELD, NAME_FIELD, STATE_KEY_FIELD, KEY_FIELD, DESCRIPTION_FIELD},
            null);

        DomainEvent expectedDbDomainEvent = buildDomainEvent(
            uuid,
            ID.toString(),
            TYPE_KEY,
            UPDATE,
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME, STATE_KEY_FIELD, STATE, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION),
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME_NEW, STATE_KEY_FIELD, STATE_NEW, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION_NEW)
        );

        verify(eventPublisher).publish(DB.name(), expectedDbDomainEvent);

    }

    @Test
    @SneakyThrows
    public void onFlushDirty_processEntityWithoutTableAnnotation() {

        String config = readConfigFile("/dbSourceConfigEmptyEntityName.yml");
        DbSourceConfig sourceConfig = objectMapper.readValue(config, DbSourceConfig.class);

        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        doReturn(null).when(databaseFilter).lepFiltering(any(), any(), any());
        doReturn(null).when(databaseDslFilter).lepFiltering(any(), any(), any());
        doReturn(new MetamodelMock()).when(entityManager).getMetamodel();

        databaseSourceInterceptor.onFlushDirty(
            buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, false),
            ID,
            new Object[]{TYPE_KEY, NAME_NEW, STATE_NEW, KEY, DESCRIPTION_NEW},
            new Object[]{TYPE_KEY, NAME, STATE, KEY, DESCRIPTION},
            new String[]{TYPE_KEY_FIELD, NAME_FIELD, STATE_KEY_FIELD, KEY_FIELD, DESCRIPTION_FIELD},
            null);

        DomainEvent expectedDbDomainEvent = buildDomainEvent(
            uuid,
            ID.toString(),
            TYPE_KEY,
            UPDATE,
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME, STATE_KEY_FIELD, STATE, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION),
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME_NEW, STATE_KEY_FIELD, STATE_NEW, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION_NEW)
        );

        verify(eventPublisher).publish(DB.name(), expectedDbDomainEvent);

    }

    @Test
    @SneakyThrows
    public void onFlushDirty_processEntityWithTableAnnotation() {

        String config = readConfigFile("/dbSourceConfig.yml");
        DbSourceConfig sourceConfig = objectMapper.readValue(config, DbSourceConfig.class);

        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        doReturn(null).when(databaseFilter).lepFiltering(any(), any(), any());
        doReturn(null).when(databaseDslFilter).lepFiltering(any(), any(), any());

        databaseSourceInterceptor.onFlushDirty(
            buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, true),
            ID,
            new Object[]{TYPE_KEY, NAME_NEW, STATE_NEW, KEY, DESCRIPTION_NEW},
            new Object[]{TYPE_KEY, NAME, STATE, KEY, DESCRIPTION},
            new String[]{TYPE_KEY_FIELD, NAME_FIELD, STATE_KEY_FIELD, KEY_FIELD, DESCRIPTION_FIELD},
            null);

        DomainEvent expectedDbDomainEvent = buildDomainEvent(
            uuid,
            ID.toString(),
            TYPE_KEY,
            UPDATE,
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME, STATE_KEY_FIELD, STATE, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION),
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME_NEW, STATE_KEY_FIELD, STATE_NEW, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION_NEW)
        );

        verify(eventPublisher).publish(DB.name(), expectedDbDomainEvent);

    }

    @Test
    @SneakyThrows
    public void onSave() {
        String config = readConfigFile("/dbSourceConfig.yml");
        DbSourceConfig sourceConfig = objectMapper.readValue(config, DbSourceConfig.class);

        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        doReturn(null).when(databaseFilter).lepFiltering(any(), any(), any());
        doReturn(null).when(databaseDslFilter).lepFiltering(any(), any(), any());

        databaseSourceInterceptor.onSave(
            buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, true),
            ID,
            new Object[]{TYPE_KEY, NAME_NEW, STATE_NEW, KEY, DESCRIPTION_NEW},
            new String[]{TYPE_KEY_FIELD, NAME_FIELD, STATE_KEY_FIELD, KEY_FIELD, DESCRIPTION_FIELD},
            null);

        DomainEvent expectedDbDomainEvent = buildDomainEvent(
            uuid,
            ID.toString(),
            TYPE_KEY,
            CREATE,
            null,
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME_NEW, STATE_KEY_FIELD, STATE_NEW, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION_NEW)
        );

        verify(eventPublisher).publish(DB.name(), expectedDbDomainEvent);
    }

    @Test
    @SneakyThrows
    public void onDelete() {
        String config = readConfigFile("/dbSourceConfig.yml");
        DbSourceConfig sourceConfig = objectMapper.readValue(config, DbSourceConfig.class);

        doReturn(sourceConfig).when(xmDomainEventConfiguration).getDbSourceConfig(TENANT_KEY, DB.getCode());
        doReturn(null).when(databaseFilter).lepFiltering(any(), any(), any());
        doReturn(null).when(databaseDslFilter).lepFiltering(any(), any(), any());

        databaseSourceInterceptor.onDelete(
            buildEntity(TYPE_KEY, NAME, STATE, KEY, DESCRIPTION, true),
            ID,
            new Object[]{TYPE_KEY, NAME, STATE, KEY, DESCRIPTION},
            new String[]{TYPE_KEY_FIELD, NAME_FIELD, STATE_KEY_FIELD, KEY_FIELD, DESCRIPTION_FIELD},
            null);

        DomainEvent expectedDbDomainEvent = buildDomainEvent(
            uuid,
            ID.toString(),
            TYPE_KEY,
            DELETE,
            Map.of(TYPE_KEY_FIELD, TYPE_KEY, NAME_FIELD, NAME, STATE_KEY_FIELD, STATE, KEY_FIELD, KEY, DESCRIPTION_FIELD, DESCRIPTION),
            null
        );

        verify(eventPublisher).publish(DB.name(), expectedDbDomainEvent);

    }

    private DbSourceConfig buildDbSourceConfig(boolean enable, Filter filter) {
        DbSourceConfig sourceConfig = new DbSourceConfig();
        sourceConfig.setEnabled(enable);
        sourceConfig.setFilter(filter);

        return sourceConfig;
    }

    private String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass().getResourceAsStream(path))))
            .lines().collect(Collectors.joining("\n"));
    }

    private Entity buildEntity(String typeKey, String name, String stateKey, String key, String description, boolean withTable) {
        Entity entity = withTable ? new EntityWithTableAnnotation() : new EntityWithoutTableAnnotation();

        entity.setTypeKey(typeKey);
        entity.setName(name);
        entity.setStateKey(stateKey);
        entity.setKey(key);
        entity.setDescription(description);

        return entity;
    }

    private DomainEvent buildDomainEvent(UUID uuid,
                                         String id,
                                         String type,
                                         DefaultDomainEventOperation operation,
                                         Map<String, Object> before,
                                         Map<String, Object> after
    ) {
        DomainEvent domainEvent = new DomainEvent();
        domainEvent.setId(uuid);
        domainEvent.setAggregateId(id);
        domainEvent.setAggregateType(type);
        domainEvent.setOperation(operation.name());

        DbDomainEventPayload domainEventPayload = new DbDomainEventPayload();
        domainEventPayload.setBefore(before);
        domainEventPayload.setAfter(after);
        domainEvent.setPayload(domainEventPayload);

        return domainEvent;
    }

}
