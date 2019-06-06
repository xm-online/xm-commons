package com.icthh.xm.commons.migration.db.tenant;

import static com.icthh.xm.commons.migration.db.Constants.DB_SCHEMA_SUFFIX;
import static com.icthh.xm.commons.migration.db.Constants.JPA_VENDOR;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides command for schema change for particular database.
 */
@Slf4j
@Component
public class SchemaChangeResolver {

    private static final String DEFAULT_COMMAND = "USE %s";
    private static final Map<String, String> DB_COMMANDS = new HashMap<>();

    private String dbSchemaChangeCommand;

    /**
     * SchemaChangeResolver constructor.
     *
     * @param env the environment
     */
    public SchemaChangeResolver(Environment env) {
        initDbCommands(env);
        String db = env.getProperty(JPA_VENDOR);
        this.dbSchemaChangeCommand = DB_COMMANDS.getOrDefault(db, DEFAULT_COMMAND);
        log.info("Database {} will use command '{}' for schema changing", db, dbSchemaChangeCommand);
    }

    public String getSchemaSwitchCommand() {
        return this.dbSchemaChangeCommand;
    }

    private void initDbCommands(Environment env) {
        String schemaSuffix = env.getProperty(DB_SCHEMA_SUFFIX, EMPTY);

        DB_COMMANDS.put("POSTGRESQL", "SET search_path TO %s" + schemaSuffix);
        DB_COMMANDS.put("ORACLE", "ALTER SESSION SET CURRENT_SCHEMA = %s" + schemaSuffix);
        DB_COMMANDS.put("H2", DEFAULT_COMMAND + schemaSuffix);
    }

}
