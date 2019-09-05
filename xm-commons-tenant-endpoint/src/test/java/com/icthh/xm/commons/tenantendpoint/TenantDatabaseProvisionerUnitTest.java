package com.icthh.xm.commons.tenantendpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.Statement;

public class TenantDatabaseProvisionerUnitTest {

    private static final String TENANT_KEY = "testKey";
    private static final String TENANT_STATE = "testState";

    private TenantDatabaseProvisioner tenantDatabaseProvisioner;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private LiquibaseProperties properties;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private DropSchemaResolver schemaDropResolver;

    @Before
    @SneakyThrows
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        tenantDatabaseProvisioner = Mockito.spy(new TenantDatabaseProvisioner(dataSource, properties,
            resourceLoader, schemaDropResolver));
    }

    @Test
    @SneakyThrows
    public void testCreateTenant() {
        doNothing().when(tenantDatabaseProvisioner).migrateSchema(any());
        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        tenantDatabaseProvisioner.createTenant(tenant);

        verify(statement, times(1)).executeUpdate(any());
    }

    @Test(expected = BusinessException.class)
    public void testCreateTenantWithWrongName() {
        Tenant tenant = new Tenant().tenantKey("_pg_error");
        tenantDatabaseProvisioner.createTenant(tenant);
    }

    @Test
    @SneakyThrows
    public void testManageTenant() {
        tenantDatabaseProvisioner.manageTenant(TENANT_KEY, TENANT_STATE);
        verify(statement, times(0)).executeUpdate(any());
    }

    @Test
    @SneakyThrows
    public void testDeleteTenant() {
        when(schemaDropResolver.getSchemaDropCommand()).thenReturn("DROP");
        tenantDatabaseProvisioner.deleteTenant(TENANT_KEY);

        verify(statement, times(1)).executeUpdate(any());
    }

    @Test(expected = BusinessException.class)
    public void testDeleteTenantWithWrongName() {
        tenantDatabaseProvisioner.deleteTenant("_pg_error");
    }
}
