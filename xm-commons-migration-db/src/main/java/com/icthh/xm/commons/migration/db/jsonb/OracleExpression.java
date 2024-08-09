package com.icthh.xm.commons.migration.db.jsonb;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;

@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:oracle:')")
public class OracleExpression implements CustomExpression {

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
        return cb.literal(object);
    }

    @Override
    public Expression<?> toJsonB(CriteriaBuilder cb, Object object) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Expression<JsonJdbcType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Expression<?> toJsonbText(CriteriaBuilder cb, Object object) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> Expression<T> toJsonbText(CriteriaBuilder cb, Object object, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Expression<JsonJdbcType> toJsonbText(CriteriaBuilder cb, Expression<?> expression) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> Expression<T> toJsonbText(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

}
