package com.icthh.xm.commons.migration.db.jsonb;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B;


@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:postgresql:')")
public class JsonbExpression implements CustomExpression {

    @Override
    public Expression<JsonJdbcType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath) {
        return jsonQuery(cb, root, column, jsonPath, JsonJdbcType.class);
    }

    @Override
    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type) {
        return cb.function(JSON_QUERY, type, root.get(column), cb.literal(jsonPath));
    }

    @Override
    public Expression<?> toExpression(CriteriaBuilder cb, Object object) {
        return toJsonB(cb, object);
    }

    @Override
    public Expression<?> toJsonB(CriteriaBuilder cb, Object object) {
        return toJsonB(cb, object, JsonJdbcType.class);
    }

    @Override
    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type) {
        return toJsonB(cb, cb.literal(object), type);
    }

    @Override
    public Expression<JsonJdbcType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        return toJsonB(cb, expression, JsonJdbcType.class);
    }

    @Override
    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        return cb.function(TO_JSON_B, type, expression);
    }

}
