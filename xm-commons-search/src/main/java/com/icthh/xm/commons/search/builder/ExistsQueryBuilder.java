package com.icthh.xm.commons.search.builder;

import com.icthh.xm.commons.search.common.Strings;

public class ExistsQueryBuilder extends AbstractQueryBuilder<ExistsQueryBuilder> {

    public static final String NAME = "exists";
    private final String fieldName;

    public ExistsQueryBuilder(String fieldName) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("field name is null or empty");
        } else {
            this.fieldName = fieldName;
        }
    }

    public String fieldName() {
        return this.fieldName;
    }

    public String getWriteableName() {
        return NAME;
    }
}
