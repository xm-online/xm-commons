package com.icthh.xm.commons.migration.db.jsonb;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;

import javax.persistence.criteria.CriteriaBuilder;

public class HibernateInlineExpression extends LiteralExpression<String> {

    public HibernateInlineExpression(CriteriaBuilder criteriaBuilder, String literal) {
        super((CriteriaBuilderImpl) criteriaBuilder, literal);
    }

    @Override
    public String render(RenderingContext renderingContext) {
        return getLiteral();
    }

}
