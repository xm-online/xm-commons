package com.icthh.xm.commons.migration.db.jsonb;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

public class CustomPostgreSQLDialect extends PostgreSQLDialect implements CustomDialect {

    public static final String JSON_QUERY_TEMPLATE = "jsonb_path_query_first(?1, ?2::jsonpath)";

    public static final String JSON_EXTRACT_PATH = "jsonb_to_string";
    public static final String JSON_EXTRACT_PATH_TEMPLATE_SIMPLE = "jsonb_extract_path_text";
    public static final String JSON_EXTRACT_PATH_TEMPLATE = "jsonb_extract_path_text(?1, VARIADIC ?2)";

    public static final String TO_JSON_B = "to_json_b";
    public static final String TO_JSON_B_TEMPLATE = "to_jsonb(?1)";

    public static final String TO_JSON_B_TEXT = "to_json_b_text";
    public static final String TO_JSON_B_TEMPLATE_TEXT = "to_jsonb(?1::text)";

    public static final String BYTEA_TYPE = "bytea";

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        BasicType<String> stringBasicType = functionContributions
            .getTypeConfiguration()
            .getBasicTypeRegistry()
            .resolve(StandardBasicTypes.STRING);

        functionContributions.getFunctionRegistry().registerPattern(JSON_QUERY, JSON_QUERY_TEMPLATE, stringBasicType);
        functionContributions.getFunctionRegistry().registerPattern(TO_JSON_B, TO_JSON_B_TEMPLATE, stringBasicType);
        functionContributions.getFunctionRegistry().registerPattern(TO_JSON_B_TEXT, TO_JSON_B_TEMPLATE_TEXT, stringBasicType);
        functionContributions.getFunctionRegistry().registerPattern(JSON_EXTRACT_PATH, JSON_EXTRACT_PATH_TEMPLATE, stringBasicType);
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        if (Types.BLOB == sqlTypeCode) {
            return BYTEA_TYPE;
        }
        return super.columnType(sqlTypeCode);
    }

    @Override
    protected String castType(int sqlTypeCode) {
        if (Types.BLOB == sqlTypeCode) {
            return BYTEA_TYPE;
        }
        return super.castType(sqlTypeCode);
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(Types.BLOB, BinaryJdbcType.INSTANCE);
    }

}
