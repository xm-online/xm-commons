package com.icthh.xm.commons.permission.service;

import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import tech.jhipster.service.filter.Filter;
import tech.jhipster.service.filter.RangeFilter;
import tech.jhipster.service.filter.StringFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Converts Criteria to JPQL statement.
 */
public class FilterConverter {

    @SuppressWarnings("unchecked")
    public static <T> QueryPart toJpql(T criteria) {
        BeanMap.Generator gen = new BeanMap.Generator();
        gen.setBean(criteria);
        gen.setContextClass(criteria.getClass());
        BeanMap beanMap = gen.create();
        return toJpql(beanMap);
    }

    private static QueryPart toJpql(Map<String, Filter> filterMap) {

        return filterMap.entrySet()
                        .stream()
                        .filter(s -> s.getValue() != null)
                        .flatMap(FilterConverter::resolveExpression)
                        .collect(QueryPart::new,
                                 QueryPart::accumulateExpression,
                                 QueryPart::combineQueryParts
                        );
    }

    private static Stream<Expression> resolveExpression(Map.Entry<String, Filter> entry) {

        Stream.Builder<Expression> expressions = Stream.builder();

        Filter<?> filter = entry.getValue();
        String fieldName = preProcessForeignKeyField(entry.getKey());

        if (filter.getEquals() != null) {
            expressions.add(new Expression(fieldName, Operation.EQUALS, filter.getEquals()));
        }
        if (filter.getNotEquals() != null) {
            expressions.add(new Expression(fieldName, Operation.NOT_EQUALS, filter.getNotEquals()));
        }
        if (filter.getSpecified() != null) {
            Boolean specified = filter.getSpecified();
            expressions.add(new Expression(fieldName,
                                           specified ? Operation.SPECIFIED : Operation.NOT_SPECIFIED,
                                           specified));
        }
        if (filter.getIn() != null) {
            expressions.add(new Expression(fieldName, Operation.IN, filter.getIn()));
        }
        if (filter.getNotIn() != null) {
            expressions.add(new Expression(fieldName, Operation.NOT_IN, filter.getNotIn()));
        }

        if (filter instanceof StringFilter && ((StringFilter) filter).getContains() != null) {
            expressions.add(new Expression(fieldName, Operation.CONTAINS, ((StringFilter) filter).getContains()));
        }
        if (filter instanceof StringFilter && ((StringFilter) filter).getDoesNotContain() != null) {
            expressions.add(new Expression(fieldName, Operation.NOT_CONTAINS, ((StringFilter) filter).getDoesNotContain()));
        }

        if (filter instanceof RangeFilter) {
            RangeFilter rangeFilter = (RangeFilter<?>) filter;
            if (rangeFilter.getGreaterThan() != null) {
                expressions.add(new Expression(fieldName,
                                               Operation.GREATER_THAN,
                                               rangeFilter.getGreaterThan()));
            }
            if (rangeFilter.getGreaterThanOrEqual() != null) {
                expressions.add(new Expression(fieldName,
                                               Operation.GREATER_OR_EQ_THAN,
                                               rangeFilter.getGreaterThanOrEqual()));
            }
            if (rangeFilter.getLessThan() != null) {
                expressions.add(new Expression(fieldName,
                                               Operation.LESS_THAN,
                                               rangeFilter.getLessThan()));
            }
            if (rangeFilter.getLessThanOrEqual() != null) {
                expressions.add(new Expression(fieldName,
                                               Operation.LESS_OR_EQ_THAN,
                                               rangeFilter.getLessThanOrEqual()));
            }
        }

        return expressions.build();
    }

    /**
     * Pre-processes foreign key field name from camelCase to snake_case.
     *
     * The reason to preprocess is because jhipster generates FK fields in snake case in DB.
     *
     * @param fieldName field name
     * @return preprocessed field name if field ends with 'Id'
     */
    private static String preProcessForeignKeyField(String fieldName) {
        if (fieldName.endsWith("Id")) {
            return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
        }
        return fieldName;
    }

    @Getter
    @ToString
    public static class QueryPart {

        public static final String JPQL_AND = " and ";

        StringBuilder query = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        Multiset<String> aliases = HashMultiset.create();

        public boolean isEmpty() {
            return query.length() == 0;
        }

        String getNextAliasName(String fieldName) {

            int cnt = aliases.count(fieldName);
            aliases.add(fieldName);

            return cnt == 0 ? fieldName : fieldName + cnt;
        }

        private static void accumulateExpression(final QueryPart qp,
                                                 final Expression expression) {

            String paramAlias = qp.getNextAliasName(expression.getFieldName());

            if (qp.isEmpty()) {
                qp.getQuery().append(expression.toJpql(paramAlias));
            } else {
                qp.getQuery().append(JPQL_AND).append(expression.toJpql(paramAlias));
            }

            if (expression.isOperationParamRequired()) {
                qp.getParams().put(paramAlias, expression.getValue());
            }
        }

        private static QueryPart combineQueryParts(final QueryPart qp1,
                                                   final QueryPart qp2) {
            qp1.getQuery().append(JPQL_AND).append(qp2.getQuery());
            qp1.getParams().putAll(qp2.getParams());
            return qp1;
        }

    }

    @Getter
    @AllArgsConstructor
    @ToString
    public static class Expression {

        public static final String JPQL_ALIAS_PREFIX = ":";

        String fieldName;
        Operation operation;
        Object value;

        boolean isOperationParamRequired() {
            return operation.isParamRequired();
        }

        String toJpql(String alias) {
            String jpql = fieldName + operation.getJpqlOp();
            if (operation.isParamRequired()) {
                jpql += JPQL_ALIAS_PREFIX + alias;
            }
            return jpql;
        }

    }

    public enum Operation {

        EQUALS(" = ", true),
        NOT_EQUALS(" <> ", true),
        SPECIFIED(" is not null ", false),
        NOT_SPECIFIED(" is null ", false),
        IN(" in ", true),
        NOT_IN(" not in ", true),
        CONTAINS(" like ", true),
        NOT_CONTAINS(" not like ", true),
        GREATER_THAN(" > ", true),
        LESS_THAN(" < ", true),
        GREATER_OR_EQ_THAN(" >= ", true),
        LESS_OR_EQ_THAN(" <= ", true);

        private final String jpqlOp;
        private final boolean paramRequired;

        Operation(String jpqlOp, boolean paramRequired) {
            this.jpqlOp = jpqlOp;
            this.paramRequired = paramRequired;
        }

        public String getJpqlOp() {
            return jpqlOp;
        }

        public boolean isParamRequired() {
            return paramRequired;
        }
    }

}
