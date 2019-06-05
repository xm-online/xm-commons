package com.icthh.xm.commons.migration.db.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DropSchemaResolver {

    @Deprecated
    private static final String DEFAULT_COMMAND = "DROP SCHEMA IF EXISTS %s CASCADE";
    private static final String DEFAULT_SQL_COMMAND = "DROP SCHEMA IF EXISTS ? CASCADE";

    @Deprecated
    private static final Map<String, String> DB_COMMANDS = new HashMap<>();
    private static final Map<String, String> DB_SQL_COMMANDS = new HashMap<>();

    static {
        DB_COMMANDS.put("POSTGRESQL", DEFAULT_COMMAND);
        DB_COMMANDS.put("H2", DEFAULT_COMMAND);

        DB_SQL_COMMANDS.put("POSTGRESQL", DEFAULT_SQL_COMMAND);
        DB_SQL_COMMANDS.put("H2", DEFAULT_SQL_COMMAND);
    }

    @Deprecated
    private final String dbDropSchemaCommand;

    private final String dbDropSchemaSqlCommand;

    /**
     * DropSchemaResolver constructor.
     * @param env the environment
     */
    public DropSchemaResolver(Environment env) {
        String db = env.getProperty("spring.jpa.database");
        this.dbDropSchemaCommand = DB_COMMANDS.getOrDefault(db, DEFAULT_COMMAND);
        this.dbDropSchemaSqlCommand = DB_SQL_COMMANDS.getOrDefault(db, DEFAULT_SQL_COMMAND);
        log.info("Database {} will use command '{}' for drop schema", db, dbDropSchemaSqlCommand);
    }

    /**
     * @deprecated use getSchemaDropSqlCommand instead
     */
    @Deprecated
    public String getSchemaDropCommand() {
        return this.dbDropSchemaCommand;
    }

    public String getSchemaDropSqlCommand() {
        return this.dbDropSchemaSqlCommand;
    }

}
