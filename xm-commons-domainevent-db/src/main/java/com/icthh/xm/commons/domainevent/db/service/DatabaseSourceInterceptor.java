package com.icthh.xm.commons.domainevent.db.service;

import com.icthh.xm.commons.domainevent.config.Column;
import com.icthh.xm.commons.domainevent.config.EntityFilter;
import com.icthh.xm.commons.domainevent.config.Query;
import com.icthh.xm.commons.domainevent.config.SourceConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.EmptyInterceptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.metamodel.Metamodel;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.CREATE;
import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.DELETE;
import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation.UPDATE;
import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource.DB;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSourceInterceptor extends EmptyInterceptor {

    private static final Map<Class<?>, String> TABLE_NAME_CACHE = new ConcurrentHashMap<>();

    private final EntityManager entityManager;

    private final XmDomainEventConfiguration xmDomainEventConfiguration;

    private final EventPublisher eventPublisher;

    private final JpaEntityMapper jpaEntityMapper;

    private final DatabaseFilter databaseFilter;

    private final DatabaseDslFilter databaseDslFilter;

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name());

        if (sourceConfig.isEnabled()) {

            String tableName = findTableName(entity);
            log.trace("onFlushDirty: tableName: {}, id: {}", tableName, id);

            JpaEntityContext context = buildJpaEntityContext(entity, id, currentState, previousState, propertyNames, types, UPDATE);

            if (isIntercepted(tableName, sourceConfig, context)) {
                DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
                eventPublisher.publish(DB.name(), dbDomainEvent);
            }
        }

        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name());

        if (sourceConfig.isEnabled()) {

            String tableName = findTableName(entity);
            log.trace("onSave: tableName: {}, id: {}", tableName, id);

            JpaEntityContext context = buildJpaEntityContext(entity, id, state, null, propertyNames, types, CREATE);

            if (isIntercepted(tableName, sourceConfig, context)) {
                DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
                eventPublisher.publish(DB.name(), dbDomainEvent);
            }
        }

        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name());

        if (sourceConfig.isEnabled()) {

            String tableName = findTableName(entity);
            log.trace("onDelete: tableName: {}, id: {}", tableName, id);

            JpaEntityContext context = buildJpaEntityContext(entity, id, null, state, propertyNames, types, DELETE);

            if (isIntercepted(tableName, sourceConfig, context)) {
                DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
                eventPublisher.publish(DB.name(), dbDomainEvent);
            }
        }

        super.onDelete(entity, id, state, propertyNames, types);
    }

    private String findTableName(Object entity) {
        Class<?> entityClass = entity.getClass();

        if (TABLE_NAME_CACHE.containsKey(entityClass)) {
            return TABLE_NAME_CACHE.get(entityClass);
        }

        Table tableAnnotation = entityClass.getAnnotation(Table.class);

        String tableName;
        if (tableAnnotation != null) {
            tableName = tableAnnotation.name();
        } else {
            log.warn("Entity: {} hasn't @Table annotation", entityClass.getSimpleName());
            tableName = findTableNameByHibernateNamingStrategy(entity);
        }
        TABLE_NAME_CACHE.put(entityClass, tableName);
        log.info("For domain class {} found corresponded table name {}", entityClass.getSimpleName(), tableName);

        return tableName;
    }

    private String findTableNameByHibernateNamingStrategy(Object entity) {
        Metamodel metamodel = entityManager.getMetamodel();

        String tableName = "";
        if (metamodel instanceof MetamodelImplementor metamodelImplementor) {
            EntityPersister entityPersister = metamodelImplementor.locateEntityPersister(entity.getClass());
            if (entityPersister instanceof AbstractEntityPersister abstractEntityPersister) {
                tableName = abstractEntityPersister.getTableName();
            }
        }
        log.info("Table name by hibernate name strategy: {}", tableName);
        return tableName;
    }

    private static JpaEntityContext buildJpaEntityContext(Object entity,
                                                          Serializable id,
                                                          Object[] currentState,
                                                          Object[] previousState,
                                                          String[] propertyNames,
                                                          Type[] types,
                                                          DefaultDomainEventOperation domainEventOperation) {
        return JpaEntityContext.builder()
            .entity(entity)
            .id(id)
            .currentState(currentState)
            .previousState(previousState)
            .propertyNames(propertyNames)
            .types(types)
            .domainEventOperation(domainEventOperation)
            .build();
    }

    /**
     *
     */
    private boolean isIntercepted(String tableName, SourceConfig sourceConfig, JpaEntityContext context) {
        if (isNull(sourceConfig.getFilter())) {
            return false;
        }

        String key = sourceConfig.getFilter().getKey();
        Boolean needFiltering = databaseFilter.lepFiltering(key, tableName, context);

        if (needFiltering == null) {
            Map<String, List<EntityFilter>> dsl = sourceConfig.getFilter().getDsl();
            log.trace("No lep executed, going to process DSL: {}", dsl);

            if (dsl.containsKey(tableName)) {
                List<EntityFilter> filters = dsl.get(tableName);

                Boolean invoked = lepInvoked(filters, tableName, context);

                return Objects.requireNonNullElseGet(invoked, () -> anyMatchQuery(filters, context));
            }
            return false;
        } else {
            return needFiltering;
        }
    }

    private Boolean lepInvoked(List<EntityFilter> filters, String tableName, JpaEntityContext context) {
        Boolean invoked = null;

        for (EntityFilter filter : filters) {
            Boolean result = databaseDslFilter.lepFiltering(filter.getKey(), tableName, context);

            if (TRUE.equals(result)) {
                return true;
            } else if (nonNull(result)) {
                invoked = result;
            }
        }
        return invoked;
    }

    private boolean anyMatchQuery(List<EntityFilter> filters, JpaEntityContext context) {
        return filters.stream()
            .anyMatch(filter -> processQuery(filter.getQuery(), context));
    }

    private boolean processQuery(Query query, JpaEntityContext context) {
        log.trace("Query: {}", query);
        if (isNull(query)) {
            return false;
        }

        List<String> propertyNames = Arrays.asList(context.getPropertyNames());
        log.trace("Property names: {}", propertyNames);

        for (Map.Entry<String, Column> columnEntry : query.getColumns().entrySet()) {

            String queryColumn = columnEntry.getKey();
            if (propertyNames.contains(queryColumn)) {

                String propertyValue = findPropertyValue(context, queryColumn);
                if (isNotEmpty(propertyValue)) {
                    Column column = columnEntry.getValue();
                    return column.match(propertyValue);
                }
            }
        }
        return false;
    }

    private String findPropertyValue(JpaEntityContext jpaEntityContext, String columnName) {

        String[] propertyNames = jpaEntityContext.getPropertyNames();
        Object[] state = jpaEntityContext.getCurrentState() != null ? jpaEntityContext.getCurrentState() : jpaEntityContext.getPreviousState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(columnName)) {
                return state[i] != null ? state[i].toString() : "null"; // if we need to check NULL value?
            }
        }
        return null;
    }

}
