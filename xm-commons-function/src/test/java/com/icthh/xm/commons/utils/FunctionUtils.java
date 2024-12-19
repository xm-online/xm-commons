package com.icthh.xm.commons.utils;

import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import lombok.experimental.UtilityClass;

/**
 * Utility class to process Functions specifications
 */
@UtilityClass
public class FunctionUtils {

    public static FunctionSpec findFunctionSpecByKey(FunctionApiSpecs specs, String functionKey) {
        return specs.getItems().stream()
            .filter(f -> functionKey.equals(f.getKey()))
            .findFirst()
            .orElse(null);
    }

    public static int countItems(FunctionApiSpecs specs) {
        return specs.getItems().size();
    }
}
