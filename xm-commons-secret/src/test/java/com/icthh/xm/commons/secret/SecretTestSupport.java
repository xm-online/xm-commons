package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantKey;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

final class SecretTestSupport {

    private SecretTestSupport() {
    }

    /** A holder with its own state, so tests do not share the thread-local tenant. */
    static TenantContextHolder tenantHolder(String tenant) {
        TenantContextHolder holder = mock(TenantContextHolder.class);
        switchTenant(holder, tenant);
        return holder;
    }

    static void switchTenant(TenantContextHolder holder, String tenant) {
        TenantContext context = mock(TenantContext.class);
        lenient().when(context.getTenantKey()).thenReturn(Optional.ofNullable(tenant).map(TenantKey::valueOf));
        lenient().when(holder.getContext()).thenReturn(context);
    }

    @SuppressWarnings("unchecked")
    static ObjectProvider<SecretStore> provider(SecretStore store) {
        ObjectProvider<SecretStore> provider = mock(ObjectProvider.class);
        lenient().when(provider.getIfAvailable()).thenReturn(store);
        return provider;
    }

    /** Every message of the cause chain, to check that a value is in none of them. */
    static String chain(Throwable t) {
        StringBuilder text = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            text.append(c).append('\n');
        }
        return text.toString();
    }
}
