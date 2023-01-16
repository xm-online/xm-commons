package com.icthh.xm.commons.domainevent.service.db;

import com.icthh.xm.commons.domainevent.config.SourceConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Service;

import javax.persistence.Table;
import java.io.Serializable;

import static com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource.DB;
import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSource extends EmptyInterceptor {

    private final XmDomainEventConfiguration xmDomainEventConfiguration;

    private final EventPublisher eventPublisher;

    private final JpaEntityMapper jpaEntityMapper;

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        String tableName = findTableName(entity);
        log.trace("onFlushDirty: tableName: {}, id: {}", tableName, id);

        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name()); // DB name?

        if (isProcess(tableName, sourceConfig)) {
            JpaEntityContext context = JpaEntityContext.builder()
                .entity(entity)
                .id(id)
                .currentState(currentState)
                .previousState(previousState)
                .propertyNames(propertyNames)
                .types(types)
                .domainEventOperation(DefaultDomainEventOperation.UPDATE)
                .build();
            DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
            eventPublisher.publish(DB.name(), dbDomainEvent);

        }

        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    //TODO entitySimpleName ??
    private String findTableName(Object entity) {
        Table tableAnnotation = entity.getClass().getAnnotation(Table.class);

        if (tableAnnotation == null) {
            log.warn(String.format("Entity: %s hasn't @Table annotation", entity.getClass().getSimpleName()));
            return translateTableNameFromClassName(entity);
        }

        return tableAnnotation.name();
    }

    private String translateTableNameFromClassName(Object entity) {
        //TODO: implement translation from Hibernate translation mechanism
        return null;
    }

    private static boolean isProcess(String tableName, SourceConfig sourceConfig) {
        return sourceConfig.isEnabled() &&
            nonNull(tableName) &&
            sourceConfig.getFilter() != null && // is correct logic?
            sourceConfig.getFilter().getDsl().containsKey(tableName);
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        String tableName = findTableName(entity);
        log.trace("onSave: tableName: {}, id: {}", tableName, id);

        SourceConfig sourceConfig = xmDomainEventConfiguration.getSourceConfig(DB.name());

        if (isProcess(tableName, sourceConfig)) {
            JpaEntityContext context = JpaEntityContext.builder()
                .entity(entity)
                .id(id)
                .currentState(state)
                .previousState(null)
                .propertyNames(propertyNames)
                .types(types)
                .domainEventOperation(DefaultDomainEventOperation.CREATE)
                .build();
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

        if (isProcess(tableName, sourceConfig)) {
            JpaEntityContext context = JpaEntityContext.builder()
                .entity(entity)
                .id(id)
                .currentState(null)
                .previousState(state)
                .propertyNames(propertyNames)
                .types(types)
                .domainEventOperation(DefaultDomainEventOperation.DELETE)
                .build();
            DomainEvent dbDomainEvent = jpaEntityMapper.map(context);
            eventPublisher.publish(DB.name(), dbDomainEvent);

        }

        super.onDelete(entity, id, state, propertyNames, types);
    }

}
