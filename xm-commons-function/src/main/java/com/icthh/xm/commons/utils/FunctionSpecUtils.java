package com.icthh.xm.commons.utils;

import com.icthh.xm.commons.domain.spec.FunctionSpec;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Utility class for function specification processing
 */
@Slf4j
@UtilityClass
public class FunctionSpecUtils {

    public static boolean filterAndLogByHttpMethod(String httpMethod, FunctionSpec functionSpec) {
        if (filterByHttpMethod(httpMethod, functionSpec)) {
            return true;
        }
        log.error("Function {} not found for http method {}", functionSpec.getKey(), httpMethod);
        return false;
    }

    public static boolean filterByHttpMethod(String httpMethod, FunctionSpec functionSpec) {
        return isEmpty(functionSpec.getHttpMethods()) || functionSpec.getHttpMethods().contains(httpMethod);
    }
}
