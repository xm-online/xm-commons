package com.icthh.xm.commons.migration.db.jsonb;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;

public interface CustomExpression {

    Expression<JsonJdbcType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath);

    <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type);

    Expression<?> toExpression(CriteriaBuilder cb, Object object);

    Expression<?> toJsonB(CriteriaBuilder cb, Object object);

    <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type);

    Expression<JsonJdbcType> toJsonB(CriteriaBuilder cb, Expression<?> expression);

    <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type);

    Expression<?> toJsonbText(CriteriaBuilder cb, Object object);

    <T> Expression<T> toJsonbText(CriteriaBuilder cb, Object object, Class<T> type);

    Expression<JsonJdbcType> toJsonbText(CriteriaBuilder cb, Expression<?> expression);

    <T> Expression<T> toJsonbText(CriteriaBuilder cb, Expression<?> expression, Class<T> type);

}
