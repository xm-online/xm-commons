package com.icthh.xm.commons.migration.db.jsonb;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;


public interface CustomExpression {

    Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath);

    <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type);

    Expression<?> toExpression(CriteriaBuilder cb, Object object);

    Expression<?> toJsonB(CriteriaBuilder cb, Object object);

    <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type);

    Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Expression<?> expression);

    <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type);

}
