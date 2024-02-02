package com.icthh.xm.commons.migration.db.jsonb;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

// todo spring 3.2.0 migration (https://docs.jboss.org/hibernate/orm/6.0/migration-guide/migration-guide.html#_dialects)
public class CustomPostgreSQLDialect extends PostgreSQLDialect implements CustomDialect { // todo spring 3.2.0 migration

    public static final String JSON_QUERY_TEMPLATE = "jsonb_path_query_first(?1, ?2::jsonpath)";

    public static final String JSON_EXTRACT_PATH = "jsonb_extract_path_text";
    public static final String JSON_EXTRACT_PATH_TEMPLATE = "jsonb_to_string";

    public static final String TO_JSON_B = "to_json_b";
    public static final String TO_JSON_B_TEMPLATE = "to_jsonb(?1)";

    public CustomPostgreSQLDialect(FunctionContributions functionContributions) {
        super();
        // registerColumnType(Types.BLOB, "bytea"); todo spring 3.2.0 migration

        BasicType<String> stringBasicType = functionContributions.getTypeConfiguration()
            .getBasicTypeRegistry().resolve(StandardBasicTypes.STRING);
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();

        functionRegistry.registerPattern(JSON_QUERY, JSON_QUERY_TEMPLATE, stringBasicType);
        functionRegistry.registerPattern(TO_JSON_B, TO_JSON_B_TEMPLATE, stringBasicType);
        functionRegistry.registerPattern(JSON_EXTRACT_PATH_TEMPLATE, JSON_EXTRACT_PATH, stringBasicType);
    }

    @Override
    public JdbcType resolveSqlTypeDescriptor(String columnTypeName, int jdbcTypeCode, int precision, int scale, JdbcTypeRegistry jdbcTypeRegistry) {
        if (jdbcTypeCode == Types.BLOB) {
            return BinaryJdbcType.INSTANCE;
        }
        return super.resolveSqlTypeDescriptor(columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry);
    }

}
