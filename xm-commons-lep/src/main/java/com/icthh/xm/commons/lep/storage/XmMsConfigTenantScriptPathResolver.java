package com.icthh.xm.commons.lep.storage;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import java.util.List;
import java.util.Map;
import org.springframework.util.AntPathMatcher;

import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_TENANT;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.commonsLepScriptsAntPathPattern;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.environmentLepScriptsAntPathPattern;

public class XmMsConfigTenantScriptPathResolver extends BaseTenantScriptPathResolver {

    @Override
    public TenantScriptStorage resolverType() {
        return TenantScriptStorage.XM_MS_CONFIG;
    }

    @Override
    public String getScriptPath(final Details details) {
        List<String> paths = PathBuilder.builder()
                .path(XM_MS_CONFIG_URL_PREFIX)
                .path("config")
                .path("tenants")
                .paths(details.getBasePath())
                .path(details.getPath())
                .build().getPaths();
        return String.join("/", paths);
    }
}
