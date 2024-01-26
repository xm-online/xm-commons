package com.icthh.xm.commons.migration.db.jsonb;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

// todo spring 3.2.0 migration (https://docs.jboss.org/hibernate/orm/6.0/migration-guide/migration-guide.html#_dialects)
public class CustomOracleDialect extends OracleDialect implements CustomDialect {

    public static final String JSON_QUERY_TEMPLATE = "json_value(?1, ?2)";

    public CustomOracleDialect(FunctionContributions functionContributions) {
        BasicType<String> stringBasicType = functionContributions.getTypeConfiguration()
            .getBasicTypeRegistry().resolve(StandardBasicTypes.STRING);
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();

        functionRegistry.registerPattern(JSON_QUERY, JSON_QUERY_TEMPLATE, stringBasicType);
    }

}
