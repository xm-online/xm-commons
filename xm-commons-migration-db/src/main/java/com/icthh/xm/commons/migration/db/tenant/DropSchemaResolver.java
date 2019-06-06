package com.icthh.xm.commons.migration.db.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DropSchemaResolver {

    private static final String DEFAULT_COMMAND = "DROP SCHEMA IF EXISTS %s CASCADE";

    private static final Map<String, String> DB_COMMANDS = new HashMap<>();

    static {
        DB_COMMANDS.put("POSTGRESQL", DEFAULT_COMMAND);
        DB_COMMANDS.put("H2", DEFAULT_COMMAND);
    }

    private String dbDropSchemaCommand;

    /**
     * DropSchemaResolver constructor.
     * @param env the environment
     */
    public DropSchemaResolver(Environment env) {
        String db = env.getProperty("spring.jpa.database");
        this.dbDropSchemaCommand = DB_COMMANDS.getOrDefault(db, DEFAULT_COMMAND);
        log.info("Database {} will use command '{}' for drop schema", db, dbDropSchemaCommand);
    }

    public String getSchemaDropCommand() {
        return this.dbDropSchemaCommand;
    }

}
