package com.icthh.xm.commons.lep.storage;

import com.icthh.xm.commons.lep.FileSystemUtils;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Paths;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;

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
