package com.icthh.xm.commons.migration.db.jsonb;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.JSON_EXTRACT_PATH_TEMPLATE_SIMPLE;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B_TEXT;


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

    @Override
    public Expression<?> toJsonbText(CriteriaBuilder cb, Object object) {
        return toJsonbText(cb, object, JsonJdbcType.class);
    }

    @Override
    public <T> Expression<T> toJsonbText(CriteriaBuilder cb, Object object, Class<T> type) {
        return toJsonbText(cb, cb.literal(object), type);
    }

    @Override
    public Expression<JsonJdbcType> toJsonbText(CriteriaBuilder cb, Expression<?> expression) {
        return toJsonbText(cb, expression, JsonJdbcType.class);
    }

    @Override
    public <T> Expression<T> toJsonbText(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        return cb.function(TO_JSON_B_TEXT, type, expression);
    }

    public Expression<String> jsonbToString(CriteriaBuilder cb, Root<?> root, String column, String... jsonPath) {
        Expression[] params = Stream.concat(
                Stream.of(root.get(column)),
                Arrays.stream(jsonPath).map(cb::literal)
            )
            .toArray(Expression[]::new);

        return cb.function(JSON_EXTRACT_PATH_TEMPLATE_SIMPLE, String.class, params);
    }

    public List<? extends Expression<?>> toJsonbCollection(Collection<?> collection, Function<Object, Expression<?>> converter) {
        return collection.stream()
            .map(converter)
            .toList();
    }
}
