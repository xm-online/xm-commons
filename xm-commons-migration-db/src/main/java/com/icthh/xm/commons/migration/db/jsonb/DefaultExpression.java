package com.icthh.xm.commons.migration.db.jsonb;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;

@Component
@ConditionalOnMissingBean(CustomExpression.class)
public class DefaultExpression implements CustomExpression {

    @Override
    public Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Expression<?> toExpression(CriteriaBuilder cb, Object object) {
        throw new NotImplementedException("Not implemented yet");
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
