package com.icthh.xm.commons.domainevent.service.db.impl;

import com.icthh.xm.commons.domainevent.domain.DbDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.DomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.domainevent.service.db.JpaEntityMapper;
import com.icthh.xm.commons.domainevent.service.db.TypeKeyAwareEntityResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@LepService(group = "event.db.mapper")
public class TypeKeyAwareJpaEntityMapper implements JpaEntityMapper {

    public static final String TYPE_KEY = "typeKey";

    private final DomainEventFactory domainEventFactory;

    @Override
    @LogicExtensionPoint(value = "TypeKey", resolver = TypeKeyAwareEntityResolver.class)
    public DomainEvent map(JpaEntityContext jpaEntityContext) {

        DomainEventPayload dbDomainEventPayload = buildDomainEventPayload(
            jpaEntityContext.getCurrentState(),
            jpaEntityContext.getPreviousState(),
            jpaEntityContext.getPropertyNames()
        );

        return domainEventFactory.build(
            jpaEntityContext.getDomainEventOperation(),
            jpaEntityContext.getId().toString(), // what is composite id?
            aggregateType(jpaEntityContext),
            dbDomainEventPayload
        );
    }

    private String aggregateType(JpaEntityContext jpaEntityContext) {

        String[] propertyNames = jpaEntityContext.getPropertyNames();
        Object[] state = jpaEntityContext.getCurrentState() != null ? jpaEntityContext.getCurrentState() : jpaEntityContext.getPreviousState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(TYPE_KEY)) {
                return state[i].toString();
            }
        }
        return StringUtils.EMPTY;
    }

    DomainEventPayload buildDomainEventPayload(Object[] currentState, Object[] previousState, String[] propertyNames) {
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();

        for (int i = 0; i < propertyNames.length; i++) {
            String propertyName = propertyNames[i];

            before.put(propertyName, previousState[i]);
            after.put(propertyName, currentState[i]);
        }

        DbDomainEventPayload domainEventPayload = new DbDomainEventPayload();
        domainEventPayload.setBefore(before);
        domainEventPayload.setAfter(after);

        return domainEventPayload;
    }
}
