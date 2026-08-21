package com.icthh.xm.commons.migration.db.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class XmMultiTenantFlywayUnitTest {

    private final XmMultiTenantFlyway subject = new XmMultiTenantFlyway();

    @Test
    public void whenShouldRunFalse_thenSkipMigration() throws Exception {
        subject.setShouldRun(false);
        subject.afterPropertiesSet();
    }

    @Test(expected = IllegalStateException.class)
    public void whenDataSourceNull_thenThrowIllegalState() throws Exception {
        subject.setSchemas(List.of("tenant1"));
        subject.afterPropertiesSet();
    }

    @Test
    public void whenSchemasEmpty_thenSkipMigration() throws Exception {
        subject.setDataSource(mock(DataSource.class));
        subject.afterPropertiesSet();
    }

    @Test
    public void whenSchemasProvided_thenRunMigrationPerSchema() throws Exception {
        subject.setDataSource(mock(DataSource.class));
        subject.setSchemas(List.of("tenant1", "tenant2"));

        Flyway flywayMock = mock(Flyway.class);
        FluentConfiguration config = mockConfig(flywayMock);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            flywayStatic.when(Flyway::configure).thenReturn(config);
            subject.afterPropertiesSet();
        }

        verify(flywayMock, times(2)).migrate();
    }

    @Test(expected = RuntimeException.class)
    public void whenMigrationFails_thenExceptionPropagated() throws Exception {
        subject.setDataSource(mock(DataSource.class));
        subject.setSchemas(List.of("tenant1"));

        Flyway flywayMock = mock(Flyway.class);
        doThrow(new RuntimeException("migration failed")).when(flywayMock).migrate();
        FluentConfiguration config = mockConfig(flywayMock);

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            flywayStatic.when(Flyway::configure).thenReturn(config);
            subject.afterPropertiesSet();
        }
    }

    private FluentConfiguration mockConfig(Flyway flywayMock) {
        FluentConfiguration config = mock(FluentConfiguration.class);
        when(config.dataSource(any(DataSource.class))).thenReturn(config);
        when(config.schemas(any(String.class))).thenReturn(config);
        when(config.defaultSchema(any())).thenReturn(config);
        when(config.locations((String[]) any())).thenReturn(config);
        when(config.baselineOnMigrate(anyBoolean())).thenReturn(config);
        when(config.outOfOrder(anyBoolean())).thenReturn(config);
        when(config.validateOnMigrate(anyBoolean())).thenReturn(config);
        when(config.placeholders(any())).thenReturn(config);
        when(config.load()).thenReturn(flywayMock);
        return config;
    }
}