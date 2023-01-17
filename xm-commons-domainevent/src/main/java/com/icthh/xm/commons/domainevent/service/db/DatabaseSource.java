package com.icthh.xm.commons.domainevent.service.db;

import com.icthh.xm.commons.domainevent.config.Column;
import com.icthh.xm.commons.domainevent.config.EntityFilter;
import com.icthh.xm.commons.domainevent.config.Query;
import com.icthh.xm.commons.domainevent.config.SourceConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Service;

import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource.DB;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSource extends EmptyInterceptor {

    private final XmDomainEventConfiguration xmDomainEventConfiguration;

    private final EventPublisher eventPublisher;

    private final JpaEntityMapper jpaEntityMapper;

    private final DatabaseFilter databaseFilter;

    private final DatabaseDslFilter databaseDslFilter;


    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        String tableName = findTableName(entity);
        log.trace("onFlushDirty: tableName: {}, id: {}", tableName, id);

        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name()); // DB name?

        JpaEntityContext context = JpaEntityContext.builder()
            .entity(entity)
            .id(id)
            .currentState(currentState)
            .previousState(previousState)
            .propertyNames(propertyNames)
            .types(types)
            .domainEventOperation(DefaultDomainEventOperation.UPDATE)
            .build();

        if (isProcess(tableName, sourceConfig, context)) {
            DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
            eventPublisher.publish(DB.name(), dbDomainEvent);
        }

        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        String tableName = findTableName(entity);
        log.trace("onSave: tableName: {}, id: {}", tableName, id);

        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name());

        JpaEntityContext context = JpaEntityContext.builder()
            .entity(entity)
            .id(id)
            .currentState(state)
            .previousState(null)
            .propertyNames(propertyNames)
            .types(types)
            .domainEventOperation(DefaultDomainEventOperation.CREATE)
            .build();

        if (isProcess(tableName, sourceConfig, context)) {
            DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
            eventPublisher.publish(DB.name(), dbDomainEvent);
        }

        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        String tableName = findTableName(entity);
        log.trace("onDelete: tableName: {}, id: {}", tableName, id);

        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name());

        JpaEntityContext context = JpaEntityContext.builder()
            .entity(entity)
            .id(id)
            .currentState(null)
            .previousState(state)
            .propertyNames(propertyNames)
            .types(types)
            .domainEventOperation(DefaultDomainEventOperation.DELETE)
            .build();

        if (isProcess(tableName, sourceConfig, context)) {
            DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
            eventPublisher.publish(DB.name(), dbDomainEvent);
        }

        super.onDelete(entity, id, state, propertyNames, types);
    }

    //TODO entitySimpleName ??
    private String findTableName(Object entity) {
        Table tableAnnotation = entity.getClass().getAnnotation(Table.class);

        if (tableAnnotation == null) {
            log.warn(String.format("EntityFilter: %s hasn't @Table annotation", entity.getClass().getSimpleName()));
            return translateTableNameFromClassName(entity);
        }
        return tableAnnotation.name();
    }

    private String translateTableNameFromClassName(Object entity) {
        //TODO: implement translation from Hibernate translation mechanism
        return null;
    }

    private boolean isProcess(String tableName, SourceConfig sourceConfig, JpaEntityContext context) {
        if (!sourceConfig.isEnabled() && Objects.isNull(sourceConfig.getFilter())) {
            return false;
        }

        String key = sourceConfig.getFilter().getKey();
        Boolean needFiltering = databaseFilter.lepFiltering(key, tableName, context);

        if (needFiltering == null) {
            // no lep executed, going to calculate DSL
            Map<String, List<EntityFilter>> dsl = sourceConfig.getFilter().getDsl();

            if (dsl.containsKey(tableName)) {
                List<EntityFilter> filters = dsl.get(tableName);

                if (lepInvoked(filters, tableName, context)) {
                    return entityFilterLepExecute(filters, tableName, context);
                }
                return anyMatchQuery(filters, context);
            }
            return false;
        } else {
            return needFiltering;
        }
    }

    private boolean lepInvoked(List<EntityFilter> filters, String tableName, JpaEntityContext context) {
        return filters.stream()
            .anyMatch(filter -> databaseDslFilter.lepFiltering(filter.getKey(), tableName, context) != null);
    }

    private boolean entityFilterLepExecute(List<EntityFilter> filters, String tableName, JpaEntityContext context) {
        return filters.stream()
            .anyMatch(filter -> Boolean.TRUE.equals(databaseDslFilter.lepFiltering(filter.getKey(), tableName, context)));
    }

    private boolean anyMatchQuery(List<EntityFilter> filters, JpaEntityContext context) {
        return filters.stream()
            .anyMatch(filter -> processQuery(filter.getQuery(), context));
    }

    private boolean processQuery(Query query, JpaEntityContext context) {
        if (Objects.isNull(query)) {
            return false;
        }

        List<String> propertyNames = Arrays.asList(context.getPropertyNames());

        for (Map.Entry<String, Column> columnEntry : query.getColumns().entrySet()) {

            String queryColumn = columnEntry.getKey();
            if (propertyNames.contains(queryColumn)) {

                String propertyValue = findPropertyValue(context, queryColumn);
                if (StringUtils.isNotEmpty(propertyValue)) {
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
                return state[i].toString();
            }
        }
        return null;
    }

}
