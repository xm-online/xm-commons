package com.icthh.xm.commons.migration.db.jsonb;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

public class CustomOracleDialect extends OracleDialect implements CustomDialect {

    public static final String JSON_QUERY_TEMPLATE = "json_value(?1, ?2)";

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        BasicType<String> stringBasicType = functionContributions
            .getTypeConfiguration()
            .getBasicTypeRegistry()
            .resolve(StandardBasicTypes.STRING);

        functionContributions.getFunctionRegistry().registerPattern(JSON_QUERY, JSON_QUERY_TEMPLATE, stringBasicType);
    }

}
