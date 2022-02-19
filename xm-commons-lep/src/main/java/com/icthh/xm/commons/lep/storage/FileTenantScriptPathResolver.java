package com.icthh.xm.commons.lep.storage;

import com.icthh.xm.commons.lep.FileSystemUtils;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Paths;
import java.util.List;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

public class FileTenantScriptPathResolver extends BaseTenantScriptPathResolver {

    @Override
    public TenantScriptStorage resolverType() {
        return TenantScriptStorage.FILE;
    }

    @Override
    public String getScriptPath(final Details details) {

        String[] paths = PathBuilder.builder()
                .path("config")
                .path("tenants")
                .paths(details.getBasePath())
                .build().asArray();

        String lepDir = Paths.get(getBaseDir(), paths).toString();
        String path = "/" + details.getPath();

        if (SystemUtils.IS_OS_WINDOWS) {
            return "file:///" + lepDir + FilenameUtils.separatorsToSystem(path);
        }

        return "file://" + lepDir + FilenameUtils.separatorsToSystem(path);
    }

    public String getBaseDir() {
        return FileSystemUtils.getAppHomeDir();
    }
}
