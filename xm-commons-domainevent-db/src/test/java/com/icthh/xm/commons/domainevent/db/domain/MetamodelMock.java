package com.icthh.xm.commons.domainevent.db.domain;

import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Set;

public class MetamodelMock implements Metamodel {

    @Override
    public EntityType<?> entity(String s) {
        return null;
    }

    @Override
    public <X> EntityType<X> entity(Class<X> aClass) {
        return null;
    }

    @Override
    public <X> ManagedType<X> managedType(Class<X> aClass) {
        return null;
    }

    @Override
    public <X> EmbeddableType<X> embeddable(Class<X> aClass) {
        return null;
    }

    @Override
    public Set<ManagedType<?>> getManagedTypes() {
        return Set.of();
    }

    @Override
    public Set<EntityType<?>> getEntities() {
        return Set.of();
    }

    @Override
    public Set<EmbeddableType<?>> getEmbeddables() {
        return Set.of();
    }
}
