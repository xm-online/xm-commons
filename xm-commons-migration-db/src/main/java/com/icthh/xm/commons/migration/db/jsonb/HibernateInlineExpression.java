package com.icthh.xm.commons.migration.db.jsonb;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;

import java.util.Collection;
import java.util.List;

// todo spring 3.2.0 migration: LiteralExpression deprecated
public class HibernateInlineExpression implements Expression<String> {

    private final CriteriaBuilder criteriaBuilder;
    private final String literal;

    public HibernateInlineExpression(CriteriaBuilder criteriaBuilder, String literal) {
        this.criteriaBuilder = criteriaBuilder;
        this.literal = literal;
//        super((CriteriaBuilderImpl) criteriaBuilder, literal);
    }

    @Override
    public Predicate isNull() {
        return null;
    }

    @Override
    public Predicate isNotNull() {
        return null;
    }

    @Override
    public Predicate in(Object... values) {
        return null;
    }

    @Override
    public Predicate in(Expression<?>... values) {
        return null;
    }

    @Override
    public Predicate in(Collection<?> values) {
        return null;
    }

    @Override
    public Predicate in(Expression<Collection<?>> values) {
        return null;
    }

    @Override
    public <X> Expression<X> as(Class<X> type) {
        return null;
    }

    @Override
    public Selection<String> alias(String name) {
        return null;
    }

    @Override
    public boolean isCompoundSelection() {
        return false;
    }

    @Override
    public List<Selection<?>> getCompoundSelectionItems() {
        return null;
    }

    @Override
    public Class<? extends String> getJavaType() {
        return null;
    }

    @Override
    public String getAlias() {
        return null;
    }

//    @Override
//    public String render(RenderingContext renderingContext) {
//        return getLiteral();
//    }
}
