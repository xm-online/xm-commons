package com.icthh.xm.commons.utils;

import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Predicate;

import static com.icthh.xm.commons.utils.CollectionsUtils.nullSafe;
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

    /**
     * Predicate to filter function spec by include/exclude key, path and tag
     * @param swaggerConfig dynamic swagger configuration
     * @return predicate
     */
    public static Predicate<? super FunctionSpec> byFilters(DynamicSwaggerConfiguration swaggerConfig) {
        if (swaggerConfig == null) {
            return functionSpec -> true;
        }

        List<String> includeTags = nullSafe(swaggerConfig.getIncludeTags());
        List<String> includePaths = nullSafe(swaggerConfig.getIncludePathPatterns());
        List<String> includeKeys = nullSafe(swaggerConfig.getIncludeKeyPatterns());

        List<String> excludeTags = nullSafe(swaggerConfig.getExcludeTags());
        List<String> excludeKeys = nullSafe(swaggerConfig.getExcludeKeyPatterns());
        List<String> excludePaths = nullSafe(swaggerConfig.getExcludePathPatterns());

        return functionSpec -> {
            boolean matchesInclude = includeTags.isEmpty() && includePaths.isEmpty() && includeKeys.isEmpty()
                || checkFilters(functionSpec, includeTags, includePaths, includeKeys);
            boolean matchesExclude = checkFilters(functionSpec, excludeTags, excludePaths, excludeKeys);

            return matchesInclude && !matchesExclude;
        };
    }

    private static boolean checkFilters(FunctionSpec functionSpec, List<String> tags,
                                        List<String> pathPatterns, List<String> keyPatterns) {

        List<String> funcTags = nullSafe(functionSpec.getTags());
        return tags.stream().anyMatch(funcTags::contains) ||
            pathPatterns.stream().anyMatch(it -> functionSpec.getPath().matches(it)) ||
            keyPatterns.stream().anyMatch(it -> functionSpec.getKey().matches(it));
    }
}
