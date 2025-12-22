package com.icthh.xm.commons.domainevent.db.service.mapper.impl;

import static com.icthh.xm.commons.domainevent.db.util.ClassTypeCheckerUtil.isCollectionOrAssociation;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;

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
    
    @Value("${xm.domainevent.db.include-collections:false}")
    private boolean includeCollections;

    private TypeKeyAwareJpaEntityMapper self;

    @Override
    @LogicExtensionPoint(value = "TypeKey")
    public DomainEvent map(JpaEntityContext jpaEntityContext) {
        return self.mapByTypeKey(jpaEntityContext);
    }

    @LogicExtensionPoint(value = "TypeKey", resolver = TypeKeyAwareEntityResolver.class)
    public DomainEvent mapByTypeKey(JpaEntityContext jpaEntityContext) {

        DomainEventPayload dbDomainEventPayload = buildDomainEventPayload(jpaEntityContext);

        return domainEventFactory.build(
            jpaEntityContext.getDomainEventOperation(),
            UUID.randomUUID(),
            jpaEntityContext.getId().toString(), // what is composite id?
            jpaEntityContext.findPropertyStateValue(TYPE_KEY),
            dbDomainEventPayload
        );
    }

    /**
     * Builds a domain event payload with control over including collection properties.
     * If includeCollections is true, all properties (including collections) are included.
     * If includeCollections is false, only simple values are included.
     */
    DomainEventPayload buildDomainEventPayload(JpaEntityContext jpaEntityContext) {
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();

        for (Map.Entry<String, State> propertyNameToState : jpaEntityContext.getPropertyNameToStates().entrySet()) {
            String propertyName = propertyNameToState.getKey();
            State propertyState = propertyNameToState.getValue();
            Object currentValue = propertyState.current();
            Object previousValue = propertyState.previous();

            if (isIncludeCollections()) {
                before.put(propertyName, previousValue);
                after.put(propertyName, currentValue);
            } else {
                if (!isCollectionOrAssociation(currentValue)) {
                    after.put(propertyName, currentValue);
                }

                if (!isCollectionOrAssociation(previousValue)) {
                    before.put(propertyName, previousValue);
                }
            }
        }

        DbDomainEventPayload domainEventPayload = new DbDomainEventPayload();
        domainEventPayload.setBefore(before);
        domainEventPayload.setAfter(after);

        return domainEventPayload;
    }

    @Autowired
    public void setSelf(@Lazy TypeKeyAwareJpaEntityMapper self) {
        this.self = self;
    }

    public boolean isIncludeCollections() {
        return includeCollections;
    }
}
