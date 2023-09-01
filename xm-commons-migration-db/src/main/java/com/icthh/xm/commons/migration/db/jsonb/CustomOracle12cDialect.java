package com.icthh.xm.commons.migration.db.jsonb;

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StringType;

public class CustomOracle12cDialect extends Oracle12cDialect implements CustomDialect {

    public static final String JSON_QUERY_TEMPLATE = "json_value(?1, ?2)";

    public CustomOracle12cDialect() {
        registerFunction(JSON_QUERY, new SQLFunctionTemplate(StringType.INSTANCE, JSON_QUERY_TEMPLATE));
    }

}
