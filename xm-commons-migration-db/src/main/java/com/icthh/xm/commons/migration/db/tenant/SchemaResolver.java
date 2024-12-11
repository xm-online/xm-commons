package com.icthh.xm.commons.migration.db.tenant;

import static com.icthh.xm.commons.migration.db.Constants.DB_SCHEMA_CREATION_ENABLED;
import static com.icthh.xm.commons.migration.db.Constants.DB_SCHEMA_SUFFIX;
import static com.icthh.xm.commons.migration.db.Constants.JPA_VENDOR;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.migration.db.util.DatabaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaResolver {

    private static final Set<String> SCHEMA_CREATION_EXCLUDE_SET = Set.of("ORACLE");

    private final Environment env;
    private final TenantListRepository tenantListRepository;

    public void createSchemas(DataSource dataSource) {
        Boolean schemaCreationEnabled = env.getProperty(DB_SCHEMA_CREATION_ENABLED, Boolean.class, Boolean.TRUE);
        String jpaVendor = env.getProperty(JPA_VENDOR);

        if (!schemaCreationEnabled) {
            log.info("Schema creation for {} jpa provider is disabled", jpaVendor);
            return;
        }
        if (SCHEMA_CREATION_EXCLUDE_SET.contains(jpaVendor)) {
            log.info("Schema creation for {} jpa provider is not supported", jpaVendor);
            return;
        }
        List<String> schemas = getSchemas();
        log.info("Create [{}] schemas for all tenants before liquibase migration", schemas.size());
        for (String schema : schemas) {
            try {
                DatabaseUtil.createSchema(dataSource, schema);
            } catch (Exception e) {
                log.error("Failed to create schema '{}', error: {}", schema, e.getMessage(), e);
            }
        }
    }

    public List<String> getSchemas() {
        String suffix = env.getProperty(DB_SCHEMA_SUFFIX);
        List<String> schemas = new ArrayList<>(tenantListRepository.getTenants());

        if (StringUtils.isNotBlank(suffix)) {
            return schemas.stream().map((schema) -> (schema.concat(suffix)))
                .collect(Collectors.toList());
        }
        return schemas;
    }
}
