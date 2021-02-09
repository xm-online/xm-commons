package com.icthh.xm.commons.domain.idp;

public class IdpConstants {

    public static final String IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/webapp/public/idp-config-public.yml";
    public static final String IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/idp-config-private.yml";
    public static final String PUBLIC_JWKS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/config/idp/clients/";
    public static final String JWKS_FILE_NAME_PATTERN = "{idpClientKey}-jwks-cache.json";
    public static final String IDP_CLIENT_KEY = "idpClientKey";
}
