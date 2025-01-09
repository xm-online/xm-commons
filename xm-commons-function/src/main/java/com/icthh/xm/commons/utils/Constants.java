package com.icthh.xm.commons.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // configuration
    public static final String FUNCTIONS = "functions";
    public static final String TENANT_CONFIG_DYNAMIC_CHECK_ENABLED = "dynamicPermissionCheckEnabled";

    // web constants
    public static final String POST_URLENCODED = "POST_URLENCODED";
    public static final String FUNCTION_CONTEXT = "functionContext";
    public static final String FUNCTION_CONTEXT_PATH = "/api/function-contexts/";

    // function privileges
    public static String FUNCTION_CALL_PRIVILEGE = "FUNCTION.CALL";

    // swagger
    public static final String SWAGGER_VERSION = "3.0.3";
    public static final String SWAGGER_INFO_VERSION = "0.0.1";
    public static final String SWAGGER_INFO_TITLE = "XM functions api";

}
