package com.icthh.xm.commons.migration.db.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DropSchemaResolver {

    private static final String DEFAULT_SQL_COMMAND = "DROP SCHEMA IF EXISTS ? CASCADE";

    private static final Map<String, String> DB_SQL_COMMANDS = new HashMap<>();

    static {
        DB_SQL_COMMANDS.put("POSTGRESQL", DEFAULT_SQL_COMMAND);
        DB_SQL_COMMANDS.put("H2", DEFAULT_SQL_COMMAND);
    }

    private final String dbDropSchemaSqlCommand;

    /**
     * DropSchemaResolver constructor.
     * @param env the environment
     */
    public DropSchemaResolver(Environment env) {
        String db = env.getProperty("spring.jpa.database");
        this.dbDropSchemaSqlCommand = DB_SQL_COMMANDS.getOrDefault(db, DEFAULT_SQL_COMMAND);
        log.info("Database {} will use command '{}' for drop schema", db, dbDropSchemaSqlCommand);
    }

    public String getSchemaDropSqlCommand() {
        return this.dbDropSchemaSqlCommand;
    }

}
