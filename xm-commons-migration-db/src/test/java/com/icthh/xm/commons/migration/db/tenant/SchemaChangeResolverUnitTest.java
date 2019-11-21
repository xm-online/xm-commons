package com.icthh.xm.commons.migration.db.tenant;

import static com.icthh.xm.commons.migration.db.Constants.JPA_VENDOR;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.migration.db.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

public class SchemaChangeResolverUnitTest {

    private SchemaChangeResolver resolver;

    @Mock
    private Environment environment;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(environment.getProperty(eq(Constants.DB_SCHEMA_SUFFIX), eq(""))).thenReturn("");
        resolver = new SchemaChangeResolver(environment);
    }

    @Test
    public void testDefaultSchemaResolver() {
        assertEquals("USE %s", resolver.getSchemaSwitchCommand());
    }

    @Test
    public void testPostgresqlSchemaResolver() {
        when(environment.getProperty(JPA_VENDOR)).thenReturn("POSTGRESQL");
        resolver = new SchemaChangeResolver(environment);
        assertEquals("SET search_path TO %s", resolver.getSchemaSwitchCommand());
    }

    @Test
    public void testPostgresqlSchemaResolverWithSuffix() {
        when(environment.getProperty(JPA_VENDOR)).thenReturn("POSTGRESQL");
        when(environment.getProperty(eq(Constants.DB_SCHEMA_SUFFIX), eq(""))).thenReturn("_suffix");
        resolver = new SchemaChangeResolver(environment);
        assertEquals("SET search_path TO %s_suffix", resolver.getSchemaSwitchCommand());
    }

    @Test
    public void testOracleSchemaResolver() {
        when(environment.getProperty(JPA_VENDOR)).thenReturn("ORACLE");
        resolver = new SchemaChangeResolver(environment);
        assertEquals("ALTER SESSION SET CURRENT_SCHEMA = %s", resolver.getSchemaSwitchCommand());
    }

    @Test
    public void testOracleSchemaResolverWithSuffix() {
        when(environment.getProperty(JPA_VENDOR)).thenReturn("ORACLE");
        when(environment.getProperty(eq(Constants.DB_SCHEMA_SUFFIX), eq(""))).thenReturn("_suffix");
        resolver = new SchemaChangeResolver(environment);
        assertEquals("ALTER SESSION SET CURRENT_SCHEMA = %s_suffix", resolver.getSchemaSwitchCommand());
    }

    @Test
    public void testH2SchemaResolver() {
        when(environment.getProperty(JPA_VENDOR)).thenReturn("H2");
        resolver = new SchemaChangeResolver(environment);
        assertEquals("USE %s", resolver.getSchemaSwitchCommand());
    }

    @Test
    public void testH2SchemaResolverWithSuffix() {
        when(environment.getProperty(JPA_VENDOR)).thenReturn("H2");
        when(environment.getProperty(eq(Constants.DB_SCHEMA_SUFFIX), eq(""))).thenReturn("_suffix");
        resolver = new SchemaChangeResolver(environment);
        assertEquals("USE %s_suffix", resolver.getSchemaSwitchCommand());
    }

}
