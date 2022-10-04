package com.icthh.xm.commons.lep;

/**
 * The {@link TenantScriptStorage} class.
 */
public enum TenantScriptStorage {

    CLASSPATH, XM_MS_CONFIG, FILE;

    /**
     * URL prefix for environment commons.
     */
    public static final String URL_PREFIX_COMMONS_ENVIRONMENT = "/commons/environment";

    /**
     * URL prefix for tenant commons.
     */
    public static final String URL_PREFIX_COMMONS_TENANT = "/commons/tenant";

}
