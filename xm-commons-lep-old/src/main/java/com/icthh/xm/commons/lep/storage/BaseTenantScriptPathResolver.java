package com.icthh.xm.commons.lep.storage;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_TENANT;
import static java.util.Arrays.asList;

public abstract class BaseTenantScriptPathResolver implements TenantScriptPathResolver {

    @Override
    public String resolvePath(String tenantKey, String appName, String path) {
        Details details = getDetails(tenantKey, appName, path);
        return getScriptPath(details);
    }

    /**
     * @param tenantKey tenant key
     * @param appName   application name
     * @param path      lep path
     * @return lep path details including base path and lep path
     */
    public Details getDetails(final String tenantKey, final String appName, final String path) {
        if (path.startsWith(URL_PREFIX_COMMONS_ENVIRONMENT)) {
            return new Details(asList("commons", "lep"), path.substring(URL_PREFIX_COMMONS_ENVIRONMENT.length()));
        } else if (path.startsWith(URL_PREFIX_COMMONS_TENANT)) {
            return new Details(asList(tenantKey.toUpperCase(), "commons", "lep"), path.substring(URL_PREFIX_COMMONS_TENANT.length()));
        } else {
            return new Details(asList(tenantKey.toUpperCase(), appName, "lep"), path);
        }
    }


    protected abstract String getScriptPath(Details details);

    @Getter
    public static class Details {
        private final List<String> basePath;
        private final String path;

        public Details(List<String> basePath, String path) {
            this.basePath = basePath;
            this.path = path.startsWith("/") ? path.substring(1) : path;
        }
    }

    @Builder
    public static class PathBuilder {
        @Singular
        @Getter
        private final List<String> paths;

        public String[] asArray() {
            return paths.toArray(new String[0]);
        }
    }

}
