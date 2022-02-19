package com.icthh.xm.commons.lep.storage;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Paths;

@RequiredArgsConstructor
public class FileTenantScriptPathResolver extends BaseTenantScriptPathResolver {

    private final String baseDir;

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

        String lepDir = Paths.get(baseDir, paths).toString();
        String path = "/" + details.getPath();

        if (SystemUtils.IS_OS_WINDOWS) {
            return "file:///" + lepDir + FilenameUtils.separatorsToSystem(path);
        }

        return "file://" + lepDir + FilenameUtils.separatorsToSystem(path);
    }

}
