package com.icthh.xm.commons.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.Optional;

import static com.icthh.xm.commons.utils.DataSpecConstants.TENANT_NAME;

@Getter
@AllArgsConstructor
public enum SpecPathPatternEnum {

    SPEC_PATH_PATTERN("/config/tenants/{tenantName}/{folder}.yml"),
    SPEC_FOLDER_PATH_PATTERN("/config/tenants/{tenantName}/{folder}/*.yml"),
    JSON_CONFIG_PATH_PATTERN("/config/tenants/{tenantName}/{folder}/**/*.json");

    private final String pathPattern;
    private static final String FOLDER_REPLACE_KEY = "{folder}";
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public static String findTenantName(String path, String folder) {
        return getByPath(path, folder)
            .map(SpecPathPatternEnum::getPathPattern)
            .map(pattern -> getTenantName(pattern, path, folder))
            .orElseThrow(() -> new IllegalArgumentException("Could not execute tenantName from path: " + path));
    }

    public static Optional<SpecPathPatternEnum> getByPath(String path, String folder) {
        return Arrays.stream(values())
            .filter(pattern -> pattern.match(path, folder))
            .findFirst();
    }

    public String getTenantName(String path, String folder) {
        return getTenantName(this.pathPattern, path, folder);
    }

    public boolean match(String path, String folder) {
        return matcher.match(prepare(this.pathPattern, folder), path);
    }

    private static String prepare(String pathPattern, String folder) {
        return pathPattern.replace(FOLDER_REPLACE_KEY, folder);
    }

    private static String getTenantName(String pattern, String path, String folder) {
        return matcher.extractUriTemplateVariables(prepare(pattern, folder), path).get(TENANT_NAME);
    }
}
