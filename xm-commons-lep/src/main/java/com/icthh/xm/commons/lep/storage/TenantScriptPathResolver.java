package com.icthh.xm.commons.lep.storage;

import com.icthh.xm.commons.lep.TenantScriptStorage;

public interface TenantScriptPathResolver {

    /**
     * According to path prefix, lep can be shared either between all tenants (environment commons LEPs),
     * or all applications under a single tenant (tenant commons LEPs), or can be used for a single application
     * (application LEPs). Based on lep type and {@link TenantScriptStorage} type, absolute LEP path is returned
     *
     * @param tenantKey tenant key
     * @param appName   application name
     * @param path      lep path
     * @return absolute lep path
     */
    String resolvePath(final String tenantKey, final String appName, final String path);

    TenantScriptStorage resolverType();

}
