package com.icthh.xm.commons.domainevent.db.service.mapper.impl;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.db.domain.State;
import com.icthh.xm.commons.domainevent.db.service.mapper.JpaEntityMapper;
import com.icthh.xm.commons.domainevent.db.lep.TypeKeyAwareEntityResolver;
import com.icthh.xm.commons.domainevent.domain.DbDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.DomainEventPayload;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventFactory;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@IgnoreLogginAspect
@RequiredArgsConstructor
@LepService(group = "event.db.mapper")
public class TypeKeyAwareJpaEntityMapper implements JpaEntityMapper {

    public static final String TYPE_KEY = "typeKey";

    private final DomainEventFactory domainEventFactory;

    @Override
    @LogicExtensionPoint(value = "TypeKey", resolver = TypeKeyAwareEntityResolver.class)
    public DomainEvent map(JpaEntityContext jpaEntityContext) {

        DomainEventPayload dbDomainEventPayload = buildDomainEventPayload(jpaEntityContext);

        return domainEventFactory.build(
            jpaEntityContext.getDomainEventOperation(),
            UUID.randomUUID(),
            jpaEntityContext.getId().toString(), // what is composite id?
            jpaEntityContext.findPropertyStateValue(TYPE_KEY),
            dbDomainEventPayload
        );
    }

    DomainEventPayload buildDomainEventPayload(JpaEntityContext jpaEntityContext) {
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();

        for (Map.Entry<String, State> propertyNameToState : jpaEntityContext.getPropertyNameToStates().entrySet()) {
            String propertyName = propertyNameToState.getKey();
            State propertyState = propertyNameToState.getValue();

            before.put(propertyName, propertyState.previous());
            after.put(propertyName, propertyState.current());
        }

        DbDomainEventPayload domainEventPayload = new DbDomainEventPayload();
        domainEventPayload.setBefore(before);
        domainEventPayload.setAfter(after);

        return domainEventPayload;
    }

}
