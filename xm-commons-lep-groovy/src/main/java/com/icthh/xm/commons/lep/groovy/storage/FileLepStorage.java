package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;

public class FileLepStorage implements LepStorage {

    private static final String TENANT_PREFIX = "/config/tenants/";

    private final String tenant;
    private final String appName;
    private final Map<String, XmLepConfigFile> defaultLeps;
    private final TenantAliasService tenantAliasService;
    private final List<String> baseDirs;
    private final String baseDir;

    public FileLepStorage(String tenant,
                          String appName,
                          Map<String, XmLepConfigFile> defaultLeps,
                          TenantAliasService tenantAliasService,
                          LepPathResolver lepPathResolver,
                          String baseDir) {
        this.tenant = tenant;
        this.appName = appName;
        this.defaultLeps = defaultLeps;
        this.tenantAliasService = tenantAliasService;
        this.baseDir = baseDir;

        this.baseDirs = lepPathResolver.getLepBasePaths(tenant).stream()
            .map(path -> baseDir + TENANT_PREFIX + path)
            .collect(toList());
        reverse(this.baseDirs);
    }

    @Override
    public void forEach(Consumer<XmLepConfigFile> action) {
        Map<String, XmLepConfigFile> leps = new HashMap<>(defaultLeps);
        this.baseDirs.stream().map(File::new).filter(File::exists).forEach(rootDir -> {
            Collection<File> files = FileUtils.listFiles(rootDir, new String[]{"groovy"}, true);
            files.forEach(file -> {
                toXmLepConfigFile(rootDir, file, leps);
            });
        });
        leps.values().forEach(action);
    }

    private static void toXmLepConfigFile(File rootDir, File file, Map<String, XmLepConfigFile> leps) {
        String path = file.getAbsolutePath().substring(rootDir.getAbsolutePath().length());
        leps.put(path, toXmLepConfigFile(path, file));
    }

    private static XmLepConfigFile toXmLepConfigFile(String path, File value) {
        return new XmLepConfigFile(path, () -> new FileInputStream(value)) {
            @Override
            public long getLastModified() {
                return value.lastModified();
            }
        };
    }

    @Override
    public XmLepConfigFile getByPath(String path) {
        Optional<File> file = findFile(path + FILE_EXTENSION).filter(File::exists);
        return file.map(value -> toXmLepConfigFile(path, value)).orElse(null);
    }

    @Override
    public boolean isExists(String path) {
        return findFile(path + FILE_EXTENSION).isPresent();
    }

    private Optional<File> findFile(String path) {
        List<String> parentKeys = tenantAliasService.getTenantAliasTree().getParentKeys(tenant);
        parentKeys.add(0, tenant);
        return parentKeys.stream()
            .map(tenantKey -> replaceTenantName(TENANT_PREFIX + path, tenantKey))
            .map(it -> baseDir + it)
            .map(File::new)
            .filter(File::exists)
            .findFirst();
    }

    private String replaceTenantName(String path, String targetTenant) {
        path = path.replace(getLepPath(tenant, appName), getLepPath(targetTenant, appName));
        path = path.replace(getLepPath(tenant, "commons"), getLepPath(targetTenant, "commons"));
        return path;
    }

    private String getLepPath(String tenant, String subfolder) {
        return TENANT_PREFIX + tenant + "/" + subfolder + "/lep/";
    }

    @Override
    public LepConnectionCache buildCache() {
        return new EmptyLepConnectionCache();
    }
}
