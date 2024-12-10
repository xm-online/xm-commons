package com.icthh.xm.commons.utils;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class Constants {

    // configuration
    public static final String FUNCTIONS = "functions";
    public static final String TENANT_CONFIG_DYNAMIC_CHECK_ENABLED = "dynamicPermissionCheckEnabled";

    // web constants
    public static final String POST_URLENCODED = "POST_URLENCODED";
    public static final String UPLOAD = "/upload";
    public static final String FUNCTION_CONTEXT = "functionContext";
    public static final String FUNCTION_CONTEXT_PATH = "/api/function-contexts/";

    // function privileges
    public static String FUNCTION_CALL_PRIVILEGE = "FUNCTION.CALL";

    // swagger
    public static final Set<String> SUPPORTED_HTTP_METHODS = Set.of(
        "GET", "POST", "PUT", "DELETE", "PATCH", "POST_URLENCODED"
    );
    public static final Set<String> SUPPORTED_ANONYMOUS_HTTP_METHODS = Set.of("GET", "POST", "POST_URLENCODED");
    public static final Set<String> SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS = Set.of("GET", "POST");

}
