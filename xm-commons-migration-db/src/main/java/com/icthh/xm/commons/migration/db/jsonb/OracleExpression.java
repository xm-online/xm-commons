package com.icthh.xm.commons.migration.db.jsonb;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;

@Component
public class OracleExpression implements CustomExpression {

    @Override
    public Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath) {
        return jsonQuery(cb, root, column, jsonPath, JsonBinaryType.class);
    }

    @Override
    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type) {
        return cb.function(JSON_QUERY, type, root.get(column), new HibernateInlineExpression(cb, jsonPath));
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
    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

}
