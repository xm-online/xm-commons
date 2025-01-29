package com.icthh.xm.commons.migration.db.tenant.provisioner;

import static com.icthh.xm.commons.migration.db.Constants.CHANGE_LOG_PATH;
import static com.icthh.xm.commons.migration.db.Constants.DB_SCHEMA_SUFFIX;
import static com.icthh.xm.commons.migration.db.Constants.DDL_CREATE_SCHEMA;
import static com.icthh.xm.commons.migration.db.util.DatabaseUtil.executeUpdateWithAutoCommit;
import static com.icthh.xm.commons.tenant.TenantContextUtils.assertTenantKeyValid;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDatabaseProvisioner implements TenantProvisioner {

    private final DataSource dataSource;
    private final LiquibaseProperties properties;
    private final DropSchemaResolver schemaDropResolver;
    private final LiquibaseRunner liquibaseRunner;
    private final String dbSchemaSuffix;

    public TenantDatabaseProvisioner(DataSource dataSource, LiquibaseProperties properties,
        DropSchemaResolver schemaDropResolver, LiquibaseRunner liquibaseRunner, Environment env) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.schemaDropResolver = schemaDropResolver;
        this.liquibaseRunner = liquibaseRunner;
        this.dbSchemaSuffix = env.getProperty(DB_SCHEMA_SUFFIX, EMPTY);
    }

    @SneakyThrows
    @Override
    public void createTenant(final Tenant tenant) {
        String tenantKey = tenant.getTenantKey().toUpperCase();
        assertTenantKeyValid(tenantKey);

        String schema = resolveSchemaName(tenantKey);
        createSchema(schema);
        migrateSchema(schema);
    }

    private void createSchema(final String schema) throws SQLException {
        String sql = String.format(DDL_CREATE_SCHEMA, schema);
        executeUpdateWithAutoCommit(dataSource, sql);
    }

    @SneakyThrows
    protected void migrateSchema(String schema) {
        String changeLogPath = getChangelogPath();
        liquibaseRunner.runOnTenant(schema, changeLogPath);
    }

    @SneakyThrows
    @Override
    public void deleteTenant(final String tenantKey) {
        assertTenantKeyValid(tenantKey);
        String schema = resolveSchemaName(tenantKey);
        String sql = String.format(schemaDropResolver.getSchemaDropCommand(), schema);
        executeUpdateWithAutoCommit(dataSource, sql);
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with DB during manage tenant: {}, state = {}", tenantKey, state);
    }

    private String getChangelogPath() {
        return Optional.ofNullable(properties.getChangeLog())
            .filter(StringUtils::isNotEmpty)
            .orElse(CHANGE_LOG_PATH);
    }

    private String resolveSchemaName(String tenantKey) {
        return (tenantKey + dbSchemaSuffix).toUpperCase();
    }
}
