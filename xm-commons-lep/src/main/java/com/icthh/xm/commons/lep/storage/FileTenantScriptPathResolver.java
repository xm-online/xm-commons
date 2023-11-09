package com.icthh.xm.commons.lep.storage;

import static com.icthh.xm.commons.lep.FileSystemUtils.getFilePrefix;
import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Paths;

@RequiredArgsConstructor
public class FileTenantScriptPathResolver extends BaseTenantScriptPathResolver {

    private final String baseDir;

    private static final String PREFIX = getFilePrefix();

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

        return PREFIX + lepDir + FilenameUtils.separatorsToSystem(path);
    }

}
