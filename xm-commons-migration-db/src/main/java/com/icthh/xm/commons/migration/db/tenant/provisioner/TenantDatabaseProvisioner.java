package com.icthh.xm.commons.migration.db.tenant.provisioner;

import static com.icthh.xm.commons.migration.db.Constants.CHANGE_LOG_PATH;
import static com.icthh.xm.commons.migration.db.Constants.DDL_CREATE_SCHEMA;
import static com.icthh.xm.commons.migration.db.util.DatabaseUtil.executeUpdateWithAutoCommit;
import static com.icthh.xm.commons.tenant.TenantContextUtils.assertTenantKeyValid;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDatabaseProvisioner implements TenantProvisioner {

    private final DataSource dataSource;
    private final LiquibaseProperties properties;
    private final DropSchemaResolver schemaDropResolver;
    private final LiquibaseRunner liquibaseRunner;

    @SneakyThrows
    @Override
    public void createTenant(final Tenant tenant) {
        String tenantKey = tenant.getTenantKey().toUpperCase();

        assertTenantKeyValid(tenantKey);

        createSchema(tenantKey);
        migrateSchema(tenantKey);
    }

    @Override
    public void manageTenant(final String tenantKey, final String state) {
        log.info("Nothing to do with DB during manage tenant: {}, state = {}", tenantKey, state);
    }

    @SneakyThrows
    @Override
    public void deleteTenant(final String tenantKey) {
        String tenantKeyUpper = tenantKey.toUpperCase();
        assertTenantKeyValid(tenantKeyUpper);
        String sql = String.format(schemaDropResolver.getSchemaDropCommand(), tenantKeyUpper);
        executeUpdateWithAutoCommit(dataSource, sql);
    }

    private void createSchema(final String tenantKey) throws SQLException {
        String sql = String.format(DDL_CREATE_SCHEMA, tenantKey);
        executeUpdateWithAutoCommit(dataSource, sql);
    }

    @SneakyThrows
    protected void migrateSchema(String tenantKey) {
        String changeLogPath = getChangelogPath();
        liquibaseRunner.runOnTenant(tenantKey, changeLogPath);
    }

    private String getChangelogPath() {
        return Optional.ofNullable(properties.getChangeLog())
            .filter(StringUtils::isNotEmpty)
            .orElse(CHANGE_LOG_PATH);
    }
}
