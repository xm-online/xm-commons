package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * The {@link TenantScriptStorage} class.
 */
public enum TenantScriptStorage {

    CLASSPATH(TenantScriptStorage::getScriptPathClasspath, TenantScriptStorage::getDetailsClasspath),
    XM_MS_CONFIG(TenantScriptStorage::getScriptPathMsConfig, TenantScriptStorage::getDetails),
    FILE(TenantScriptStorage::getScriptPathFile, TenantScriptStorage::getDetails);

    private final Function<Details, String> getScriptPath;
    private final DetailsExtractor getDetails;

    TenantScriptStorage(Function<Details, String> getScriptPath, DetailsExtractor getDetails) {
        this.getDetails = getDetails;
        this.getScriptPath = getScriptPath;
    }


    private static String getScriptPathClasspath(final Details details) {
        List<String> paths = PathBuilder.builder()
            .path(CLASSPATH_URL_PREFIX)
            .path("lep")
            .path("custom")
            .paths(details.basePath)
            .path(details.path)
            .build().paths;
        return String.join("/", paths);
    }

    private static String getScriptPathMsConfig(final Details details) {
        List<String> paths = PathBuilder.builder()
            .path(XM_MS_CONFIG_URL_PREFIX)
            .path("config")
            .path("tenants")
            .paths(details.basePath)
            .path(details.path)
            .build().paths;
        return String.join("/", paths);
    }

    private static String getScriptPathFile(final Details details) {

        String[] paths = PathBuilder.builder()
            .path("config")
            .path("tenants")
            .paths(details.basePath)
            .build().asArray();

        String lepDir = Paths.get(FileSystemUtils.getAppHomeDir(), paths).toString();
        String path = "/" + details.path;

        if (SystemUtils.IS_OS_WINDOWS) {
            return "file:///" + lepDir + FilenameUtils.separatorsToSystem(path);
        }

        return "file://" + lepDir + FilenameUtils.separatorsToSystem(path);
    }

    /**
     * @param tenantKey tenant key
     * @param appName   application name
     * @param path      lep path
     * @return lep path details including base path and lep path
     */
    private static Details getDetails(final String tenantKey, final String appName, final String path) {
        if (path.startsWith(URL_PREFIX_COMMONS_ENVIRONMENT)) {
            return new Details(asList("commons", "lep"), path.substring(URL_PREFIX_COMMONS_ENVIRONMENT.length()));
        } else if (path.startsWith(URL_PREFIX_COMMONS_TENANT)) {
            return new Details(asList(tenantKey.toUpperCase(), "commons", "lep"), path.substring(URL_PREFIX_COMMONS_TENANT.length()));
        } else {
            return new Details(asList(tenantKey.toUpperCase(), appName, "lep"), path);
        }
    }

    private static Details getDetailsClasspath(final String tenantKey, final String appName, final String path) {
        if (path.startsWith(URL_PREFIX_COMMONS_ENVIRONMENT)) {
            return new Details(emptyList(), path.substring(URL_PREFIX_COMMONS_ENVIRONMENT.length()));
        } else if (path.startsWith(URL_PREFIX_COMMONS_TENANT)) {
            return new Details(singletonList(tenantKey.toLowerCase()), path.substring(URL_PREFIX_COMMONS_TENANT.length()));
        } else {
            return new Details(singletonList(tenantKey.toLowerCase()), path);
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
        Details details = getDetails.extract(tenantKey, appName, path);
        return getScriptPath.apply(details);
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

    @FunctionalInterface
    private interface DetailsExtractor {
        Details extract(final String tenantKey, final String appName, final String path);
    }

    @Builder
    private static class PathBuilder {
        @Singular
        private final List<String> paths;

        public String[] asArray() {
            return paths.toArray(new String[0]);
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
