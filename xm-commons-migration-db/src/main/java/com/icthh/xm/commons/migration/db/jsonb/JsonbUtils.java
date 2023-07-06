package com.icthh.xm.commons.migration.db.jsonb;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.experimental.UtilityClass;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQL95Dialect.TO_JSON_B;

/**
 * Utility class for use custom sql json functions by dialect
 */
@UtilityClass
public class JsonbUtils {

    public Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath) {
        return jsonQuery(cb, root, column, jsonPath, JsonBinaryType.class);
    }

    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type) {
        return cb.function(JSON_QUERY, type, root.get(column), new HibernateInlineExpression(cb, jsonPath));
    }

    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Object object) {
        return toJsonB(cb, object, JsonBinaryType.class);
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type) {
        return toJsonB(cb, cb.literal(object), type);
    }

    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        return toJsonB(cb, expression, JsonBinaryType.class);
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        return cb.function(TO_JSON_B, type, expression);
    }

}
