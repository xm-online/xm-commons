package com.icthh.xm.commons.domainevent.service.db.impl;

import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.domain.DbDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.DomainEventPayload;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.domainevent.service.db.JpaEntityMapper;
import com.icthh.xm.commons.domainevent.service.db.JpaEntityResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@LepService(group = "event.publisher")
public class TypeKeyAwareJpaEntityMapper implements JpaEntityMapper {

    public static final String TYPE_KEY = "typeKey";

    private final DomainEventFactory domainEventFactory;

    @Override
    @LogicExtensionPoint(value = "TypeKeyMapper", resolver = JpaEntityResolver.class)
    public DomainEvent maps(Object entity, JpaEntityContext jpaEntityContext) {

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
        return "";
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
