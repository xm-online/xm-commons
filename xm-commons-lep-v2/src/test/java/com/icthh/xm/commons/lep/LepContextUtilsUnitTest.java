package com.icthh.xm.commons.lep;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * The {@link LepContextUtilsUnitTest} class.
 */
public class LepContextUtilsUnitTest {

    private static final String TEST_TENANT_KEY = "test-tenant";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    private ContextsHolder contextsHolder;
    @Mock
    private ScopedContext threadContext;
    @Mock
    private TenantContext tenantContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void mockTenantContext(String tenantKeyValue) {
        PlainTenant tenant = new PlainTenant(TenantKey.valueOf(tenantKeyValue));
        when(tenantContext.getTenant()).thenReturn(Optional.of(tenant));
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(tenant.getTenantKey()));
    }

    @Test
    public void whenValidContextThenReturnTenantName() {
        when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        when(threadContext.getValue(eq(XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT),
                                    eq(TenantContext.class))).thenReturn(tenantContext);
        mockTenantContext(TEST_TENANT_KEY);

        assertEquals(TEST_TENANT_KEY, LepContextUtils.getTenantKey(contextsHolder));
    }

    @Test
    public void whenNoThreadContextThenThrowException() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("LEP manager thread context doesn't initialized."));

        when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(null);

        LepContextUtils.getTenantKey(contextsHolder);
    }

    @Test
    public void whenNoTenantInfoThenThrowException() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage(Matchers.startsWith("LEP manager thread context doesn't have value for var: "));

        when(contextsHolder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        when(threadContext.getValue(eq(XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT),
                                    eq(TenantContext.class))).thenReturn(null);

        assertEquals(TEST_TENANT_KEY, LepContextUtils.getTenantKey(contextsHolder));
    }

}
