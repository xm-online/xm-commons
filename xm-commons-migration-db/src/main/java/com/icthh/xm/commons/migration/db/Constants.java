package com.icthh.xm.commons.migration.db;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String DDL_CREATE_SCHEMA = "CREATE SCHEMA IF NOT EXISTS %s";
    public static final String CHANGE_LOG_PATH = "classpath:config/liquibase/master.xml";

    public static final String JPA_VENDOR = "spring.jpa.database";
    public static final String DB_SCHEMA_CREATION_ENABLED = "spring.jpa.properties.db.schema.creation.enabled";
    public static final String DB_SCHEMA_SUFFIX = "application.db-schema-suffix";
}
