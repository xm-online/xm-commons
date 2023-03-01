package com.icthh.xm.commons.domainevent.db.domain;

import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import java.util.Set;

public class MetamodelMock implements Metamodel {
    @Override
    public <X> EntityType<X> entity(Class<X> cls) {
        return null;
    }

    @Override
    public <X> ManagedType<X> managedType(Class<X> cls) {
        return null;
    }

    @Override
    public <X> EmbeddableType<X> embeddable(Class<X> cls) {
        return null;
    }

    @Override
    public Set<ManagedType<?>> getManagedTypes() {
        return null;
    }

    @Override
    public Set<EntityType<?>> getEntities() {
        return null;
    }

    @Override
    public Set<EmbeddableType<?>> getEmbeddables() {
        return null;
    }
}
