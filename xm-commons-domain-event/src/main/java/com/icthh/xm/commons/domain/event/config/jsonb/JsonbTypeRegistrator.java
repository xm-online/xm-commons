package com.icthh.xm.commons.domain.event.config.jsonb;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Properties;

import static java.lang.Boolean.TRUE;
import static org.hibernate.usertype.DynamicParameterizedType.IS_DYNAMIC;
import static org.hibernate.usertype.DynamicParameterizedType.RETURNED_CLASS;

/**
 * This class update metadata of field marked as @Jsonb. 1) Remove converter is exists. 2) Update type to JsonBinaryType
 * Operation run only on postgres (determine db by hibernate dialect, it should extends PostgreSQL95Dialect)
 */
@Slf4j
public class JsonbTypeRegistrator implements SessionFactoryBuilderFactory {

    @Override
    public SessionFactoryBuilder getSessionFactoryBuilder(final MetadataImplementor metadata,
        final SessionFactoryBuilderImplementor defaultBuilder) {
        MetadataImpl metadataImpl = (MetadataImpl) metadata;
        Dialect dialect = metadataImpl.getDatabase().getDialect();
        if (dialect instanceof PostgreSQL95Dialect) {
            log.info(
                "Run on {} dialect. Metadata will be processed. "
                    + "And field marker as @Jsonb will be replaced to jsonb type.",
                dialect);

            metadataImpl.getEntityBindings().forEach(mapping ->
                updateEntityMapping(metadataImpl, mapping.getDeclaredPropertyIterator(), mapping.getMappedClass()));
        } else {
            log.info("Run on {} dialect. Metadata will not be processed", dialect);
        }
        return defaultBuilder;
    }

    private void updateEntityMapping(MetadataImpl metadata, Iterator<Property> propertyIterator,
        Class<?> persistentClass) {
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            Class<?> fieldType = getField(persistentClass, property).getType();
            if (property.getValue() instanceof Component) {
                Component component = (Component) property.getValue();
                updateEntityMapping(metadata, component.getPropertyIterator(), fieldType);
            } else if (property.getValue() instanceof SimpleValue && isJsonb(persistentClass, property)) {
                updateMappingTypeToJsonb(metadata, property, fieldType.getCanonicalName());
            }
        }
    }

    private boolean isJsonb(Class<?> persistentClass, Property property) {
        return getField(persistentClass, property).isAnnotationPresent(Jsonb.class);
    }

    @SneakyThrows
    private Field getField(Class<?> persistentClass, Property property) {
        return persistentClass.getDeclaredField(property.getName());
    }

    private void updateMappingTypeToJsonb(MetadataImpl metadata, Property jsonProperty, String fieldClassName) {
        log.info("Set type for {} to jsonb", jsonProperty.getName());
        SimpleValue simpleValue = new SimpleValue(metadata.getTypeConfiguration().getMetadataBuildingContext());
        simpleValue.setTypeName(JsonBinaryType.class.getCanonicalName());
        if (simpleValue.getTypeParameters() == null) {
            simpleValue.setTypeParameters(new Properties());
        }
        simpleValue.getTypeParameters().setProperty(IS_DYNAMIC, TRUE.toString());
        simpleValue.getTypeParameters().setProperty(RETURNED_CLASS, fieldClassName);
        simpleValue.setJpaAttributeConverterDescriptor(null);

        ((SimpleValue) jsonProperty.getValue()).copyTypeFrom(simpleValue);
    }
}
