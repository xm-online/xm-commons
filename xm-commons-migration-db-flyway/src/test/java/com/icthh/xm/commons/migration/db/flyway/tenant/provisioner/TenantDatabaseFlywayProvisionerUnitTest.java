package com.icthh.xm.commons.migration.db.flyway.tenant.provisioner;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.flyway.FlywayRunner;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TenantDatabaseFlywayProvisionerUnitTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final FlywayProperties flywayProperties = mock(FlywayProperties.class);
    private final DropSchemaResolver dropSchemaResolver = mock(DropSchemaResolver.class);
    private final FlywayRunner flywayRunner = mock(FlywayRunner.class);
    private final Environment env = mock(Environment.class);

    @Before
    public void setUp() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    public void whenFlywayDisabledByProperty_thenSkipTenantMigration() {
        when(flywayProperties.isEnabled()).thenReturn(false);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        createProvisioner().createTenant(tenant("test"));

        verifyNoInteractions(flywayRunner);
    }

    @Test
    public void whenNoFlywayProfileActive_thenSkipTenantMigration() {
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        createProvisioner().createTenant(tenant("test"));

        verifyNoInteractions(flywayRunner);
    }

    @Test
    public void whenFlywayEnabled_thenRunTenantMigration() {
        when(flywayProperties.isEnabled()).thenReturn(true);
        when(flywayProperties.getLocations()).thenReturn(List.of("classpath:db/migration"));
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        createProvisioner().createTenant(tenant("test"));

        verify(flywayRunner).runOnTenant(eq("TEST"), any(String[].class));
    }

    private TenantDatabaseFlywayProvisioner createProvisioner() {
        return new TenantDatabaseFlywayProvisioner(
            dataSource, flywayProperties, dropSchemaResolver, flywayRunner, env, "", ""
        );
    }

    private Tenant tenant(String key) {
        Tenant tenant = new Tenant();
        tenant.setTenantKey(key);
        return tenant;
    }
}