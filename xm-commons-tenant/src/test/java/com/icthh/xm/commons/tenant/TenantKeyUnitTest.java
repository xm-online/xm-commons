package com.icthh.xm.commons.tenant;

import static com.icthh.xm.commons.tenant.TenantContextUtils.isTenantKeyValid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * The {@link TenantKeyUnitTest} class.
 */
public class TenantKeyUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testSuccessValueOf() {
        assertEquals("test", TenantKey.valueOf("test").getValue());
    }

    @Test
    public void nullKeyValueOfThrowsException() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("value can't be null");

        TenantKey.valueOf(null);
    }

    @Test
    public void constructorCreation() {
        assertFalse(TenantKey.valueOf("test").isSuperTenant());
        assertTrue(TenantKey.valueOf(TenantKey.SUPER_TENANT_KEY_VALUE).isSuperTenant());
    }

    @Test
    public void nullKeyInConstructorThrowsException() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("value can't be null");

        new TenantKey(null);
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(TenantKey.class).suppress(Warning.STRICT_INHERITANCE).verify();
    }

    @Test
    public void toStringContract() {
        TenantKey testTenantKey = TenantKey.valueOf("test");
        assertEquals("value = test", testTenantKey.toString());
    }

    @Test
    public void testValidateTenantKey() {
        assertTrue(isTenantKeyValid("ValidTenant"));
        assertTrue(isTenantKeyValid("VALIDTENANT"));
        assertTrue(isTenantKeyValid("VALID_TENANT"));
        assertTrue(isTenantKeyValid("ValidTenant"));
        assertFalse(isTenantKeyValid("INVALID TENANT NAME"));
        assertFalse(isTenantKeyValid("INVALIDTENANTNAME;"));
    }

}
