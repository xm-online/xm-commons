package com.icthh.xm.commons.migration.db.tenant.provisioner;

import static com.icthh.xm.commons.migration.db.Constants.DDL_CREATE_SCHEMA;
import static com.icthh.xm.commons.migration.db.util.DatabaseUtil.executeUpdateWithAutoCommit;
import static com.icthh.xm.commons.tenant.TenantContextUtils.assertTenantKeyValid;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Set;

@Slf4j
public abstract class AbstractTenantDatabaseProvisioner implements TenantProvisioner {

    private static final Set<String> SCHEMA_CREATION_EXCLUDE_SET = Set.of("ORACLE");

    protected final DataSource dataSource;
    protected final DropSchemaResolver schemaDropResolver;
    protected final String dbSchemaSuffix;
    protected final String jpaVendor;

    protected AbstractTenantDatabaseProvisioner(DataSource dataSource,
                                                DropSchemaResolver schemaDropResolver,
                                                String dbSchemaSuffix,
                                                String jpaVendor) {
        this.dataSource = dataSource;
        this.schemaDropResolver = schemaDropResolver;
        this.dbSchemaSuffix = dbSchemaSuffix;
        this.jpaVendor = jpaVendor;
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

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with DB during manage tenant: {}, state = {}", tenantKey, state);
    }

    @SneakyThrows
    @Override
    public void deleteTenant(final String tenantKey) {
        assertTenantKeyValid(tenantKey);
        String schema = resolveSchemaName(tenantKey);
        String sql = String.format(schemaDropResolver.getSchemaDropCommand(), schema);
        executeUpdateWithAutoCommit(dataSource, sql);
    }

    protected abstract void migrateSchema(String schema) throws Exception;

    private void createSchema(final String schema) throws SQLException {
        if (jpaVendor != null && SCHEMA_CREATION_EXCLUDE_SET.contains(jpaVendor.toUpperCase())) {
            log.info("Schema creation for {} jpa provider is not supported, skipping for schema: {}", jpaVendor, schema);
            return;
        }
        String sql = String.format(DDL_CREATE_SCHEMA, schema);
        executeUpdateWithAutoCommit(dataSource, sql);
    }

    private String resolveSchemaName(String tenantKey) {
        return StringUtils.isBlank(dbSchemaSuffix) ? tenantKey.toUpperCase()
            : (tenantKey + dbSchemaSuffix).toUpperCase();
    }
}
