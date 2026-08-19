package com.icthh.xm.commons.migration.db.tenant.provisioner;

import static com.icthh.xm.commons.migration.db.Constants.CHANGE_LOG_PATH;

import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Optional;

@Slf4j
@Service
public class TenantDatabaseProvisioner extends AbstractTenantDatabaseProvisioner {

    private final LiquibaseProperties properties;
    private final LiquibaseRunner liquibaseRunner;

    public TenantDatabaseProvisioner(DataSource dataSource, LiquibaseProperties properties,
                                     DropSchemaResolver schemaDropResolver, LiquibaseRunner liquibaseRunner,
                                     @Value("${application.db-schema-suffix:}") String dbSchemaSuffix,
                                     @Value("${spring.jpa.database:}") String jpaVendor) {
        super(dataSource, schemaDropResolver, dbSchemaSuffix, jpaVendor);
        this.properties = properties;
        this.liquibaseRunner = liquibaseRunner;
    }

    @SneakyThrows
    @Override
    protected void migrateSchema(String schema) {
        String changeLogPath = Optional.ofNullable(properties.getChangeLog())
            .filter(StringUtils::isNotEmpty)
            .orElse(CHANGE_LOG_PATH);
        liquibaseRunner.runOnTenant(schema, changeLogPath);
    }
}
