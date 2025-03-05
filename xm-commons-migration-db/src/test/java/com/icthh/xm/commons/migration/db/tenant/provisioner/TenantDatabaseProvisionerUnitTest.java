package com.icthh.xm.commons.migration.db.tenant.provisioner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

public class TenantDatabaseProvisionerUnitTest {

    private static final String TENANT_KEY = "testKey";
    private static final String TENANT_STATE = "testState";
    private static final String DB_SCHEMA_SUFFIX_VALUE = "_suffix";

    private TenantDatabaseProvisioner tenantDatabaseProvisioner;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private LiquibaseProperties properties;

    @Mock
    private DropSchemaResolver schemaDropResolver;

    @Mock
    private LiquibaseRunner liquibaseRunner;

    @Before
    @SneakyThrows
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        tenantDatabaseProvisioner = Mockito.spy(new TenantDatabaseProvisioner(dataSource, properties,
            schemaDropResolver, liquibaseRunner, null));
    }

    @Test
    @SneakyThrows
    public void testCreateTenant() {
        doNothing().when(tenantDatabaseProvisioner).migrateSchema(any());
        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        tenantDatabaseProvisioner.createTenant(tenant);
        InOrder inOrder = inOrder(connection, statement);

        inOrder.verify(connection).setAutoCommit(eq(true));
        inOrder.verify(statement, times(1))
               .executeUpdate("CREATE SCHEMA IF NOT EXISTS " + TENANT_KEY.toUpperCase());
        inOrder.verify(connection).setAutoCommit(eq(false));

        verify(tenantDatabaseProvisioner).migrateSchema(TENANT_KEY.toUpperCase());
    }

    @Test
    @SneakyThrows
    public void testCreateTenantWithSuffix() {
        tenantDatabaseProvisioner = Mockito.spy(new TenantDatabaseProvisioner(dataSource, properties,
            schemaDropResolver, liquibaseRunner, DB_SCHEMA_SUFFIX_VALUE));

        doNothing().when(tenantDatabaseProvisioner).migrateSchema(any());
        Tenant tenant = new Tenant().tenantKey(TENANT_KEY);
        tenantDatabaseProvisioner.createTenant(tenant);
        InOrder inOrder = inOrder(connection, statement);

        String expectedSchema = TENANT_KEY.toUpperCase() + DB_SCHEMA_SUFFIX_VALUE.toUpperCase();
        inOrder.verify(connection).setAutoCommit(eq(true));
        inOrder.verify(statement, times(1))
            .executeUpdate("CREATE SCHEMA IF NOT EXISTS " + expectedSchema);
        inOrder.verify(connection).setAutoCommit(eq(false));

        verify(tenantDatabaseProvisioner).migrateSchema(expectedSchema);
    }

    @Test
    public void testCreateTenantWithWrongName() {

        expectedEx.expect(BusinessException.class);
        expectedEx.expectMessage("Tenant key wrong format");

        Tenant tenant = new Tenant().tenantKey("_pg_error");
        tenantDatabaseProvisioner.createTenant(tenant);

    }

    @Test
    @SneakyThrows
    public void testManageTenant() {
        tenantDatabaseProvisioner.manageTenant(TENANT_KEY, TENANT_STATE);
        verifyZeroInteractions(dataSource);
        verifyZeroInteractions(connection);
        verifyZeroInteractions(statement);
        verifyZeroInteractions(liquibaseRunner);
        verifyZeroInteractions(schemaDropResolver);
    }

    @Test
    @SneakyThrows
    public void testDeleteTenant() {
        when(schemaDropResolver.getSchemaDropCommand()).thenReturn("DROP SCHEMA IF EXISTS %s CASCADE");
        tenantDatabaseProvisioner.deleteTenant(TENANT_KEY);

        verify(statement, times(1)).executeUpdate("DROP SCHEMA IF EXISTS TESTKEY CASCADE");
    }

    @Test
    @SneakyThrows
    public void testDeleteTenantWithSuffix() {
        tenantDatabaseProvisioner = Mockito.spy(new TenantDatabaseProvisioner(dataSource, properties,
            schemaDropResolver, liquibaseRunner, DB_SCHEMA_SUFFIX_VALUE));

        when(schemaDropResolver.getSchemaDropCommand()).thenReturn("DROP SCHEMA IF EXISTS %s CASCADE");
        tenantDatabaseProvisioner.deleteTenant(TENANT_KEY);

        verify(statement, times(1)).executeUpdate("DROP SCHEMA IF EXISTS TESTKEY_SUFFIX CASCADE");
    }

    @Test
    public void testDeleteTenantWithWrongName() {
        expectedEx.expect(BusinessException.class);
        expectedEx.expectMessage("Tenant key wrong format");
        tenantDatabaseProvisioner.deleteTenant("_pg_error");
    }
}
