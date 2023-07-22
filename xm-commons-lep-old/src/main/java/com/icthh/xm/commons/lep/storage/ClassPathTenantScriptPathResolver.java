package com.icthh.xm.commons.lep.storage;

import com.icthh.xm.commons.lep.TenantScriptStorage;

import java.util.List;

import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_TENANT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

public class ClassPathTenantScriptPathResolver extends BaseTenantScriptPathResolver {

    @Override
    public TenantScriptStorage resolverType() {
        return TenantScriptStorage.CLASSPATH;
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

    @Override
    public String getScriptPath(final Details details) {
        List<String> paths = PathBuilder.builder()
                .path(CLASSPATH_URL_PREFIX)
                .path("lep")
                .path("custom")
                .paths(details.getBasePath())
                .path(details.getPath())
                .build().getPaths();
        return String.join("/", paths);
    }
}
