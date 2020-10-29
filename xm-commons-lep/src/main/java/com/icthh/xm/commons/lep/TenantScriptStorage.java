package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

/**
 * The {@link TenantScriptStorage} class.
 */
public enum TenantScriptStorage {

    CLASSPATH {
        @Override
        public String getScriptPath(final Details details) {
            List<String> lepDirList = new ArrayList<>(asList(CLASSPATH_URL_PREFIX, "lep", "custom"));
            lepDirList.addAll(details.basePath);
            lepDirList.add(details.path);
            return String.join("/", lepDirList);
        }

        @Override
        public Details getDetails(final String tenantKey, final String appName, final String path) {
            if (path.startsWith(URL_PREFIX_COMMONS_ENVIRONMENT)) {
                return new Details(emptyList(), path.substring(URL_PREFIX_COMMONS_ENVIRONMENT.length()));
            } else if (path.startsWith(URL_PREFIX_COMMONS_TENANT)) {
                return new Details(singletonList(tenantKey.toLowerCase()), path.substring(URL_PREFIX_COMMONS_TENANT.length()));
            } else {
                return new Details(singletonList(tenantKey.toLowerCase()), path);
            }
        }
    },
    XM_MS_CONFIG {
        @Override
        public String getScriptPath(final Details details) {
            List<String> lepDirList = new ArrayList<>(asList(XM_MS_CONFIG_URL_PREFIX, "config", "tenants"));
            lepDirList.addAll(details.basePath);
            lepDirList.add(details.path);
            return String.join("/", lepDirList);
        }
    },
    FILE {
        @Override
        public String getScriptPath(final Details details) {
            List<String> lepDirList = new ArrayList<>(asList("config", "tenants"));
            lepDirList.addAll(details.basePath);
            String lepDir = Paths.get(FileSystemUtils.APP_HOME_DIR, lepDirList.toArray(new String[0])).toString();
            return "file://" + lepDir + FilenameUtils.separatorsToSystem("/" + details.path);
        }
    };

    protected abstract String getScriptPath(final Details details);

    /**
     * @param tenantKey tenant key
     * @param appName   application name
     * @param path      lep path
     * @return lep path details including base path and lep path
     */
    protected Details getDetails(final String tenantKey, final String appName, final String path) {
        if (path.startsWith(URL_PREFIX_COMMONS_ENVIRONMENT)) {
            return new Details(asList("commons", "lep"), path.substring(URL_PREFIX_COMMONS_ENVIRONMENT.length()));
        } else if (path.startsWith(URL_PREFIX_COMMONS_TENANT)) {
            return new Details(asList(tenantKey.toUpperCase(), "commons", "lep"), path.substring(URL_PREFIX_COMMONS_TENANT.length()));
        } else {
            return new Details(asList(tenantKey.toUpperCase(), appName, "lep"), path);
        }
    }

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
    public String resolvePath(final String tenantKey, final String appName, final String path) {
        Details details = getDetails(tenantKey, appName, path);
        return getScriptPath(details);
    }

    @Getter
    private static class Details {
        private final List<String> basePath;
        private final String path;

        public Details(final List<String> basePath, final String path) {
            this.basePath = basePath;
            this.path = path.startsWith("/") ? path.substring(1) : path;
        }
    }

    /**
     * URL prefix for environment commons.
     */
    public static final String URL_PREFIX_COMMONS_ENVIRONMENT = "/commons/environment";

    /**
     * URL prefix for tenant commons.
     */
    public static final String URL_PREFIX_COMMONS_TENANT = "/commons/tenant";

}
