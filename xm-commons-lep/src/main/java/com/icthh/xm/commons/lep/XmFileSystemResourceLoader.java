package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.domain.TenantAliasTree;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.AntPathMatcher;

import java.util.Optional;

@RequiredArgsConstructor
public class XmFileSystemResourceLoader implements ResourceLoader {

    private final AntPathMatcher matcher = new AntPathMatcher();
    private static final String TENANT_NAME = "tenantName";
    private static final String TENANT_PREFIX = "**/config/tenants/{" + TENANT_NAME + "}/";

    private final FileSystemResourceLoader delegate;
    private final TenantAliasService tenantAliasService;
    private final String appName;

    @Override
    public Resource getResource(String location) {
        Resource resource = delegate.getResource(location);
        if (resource.exists()) {
            return resource;
        }

        return tenantAliasService.getTenantAliasTree().getParents("")
                .stream()
                .map(TenantAliasTree.TenantAlias::getKey)
                .map(delegate::getResource)
                .filter(Resource::exists)
                .findFirst()
                .orElse(resource);
    }

    public Optional<String> getTenantName(String path) {
        return Optional.of(path).filter(this::isUnderTenantFolder).map(this::extractTenantName);
    }

    private String extractTenantName(String path) {
        final String pattern = TENANT_PREFIX + appName + "/lep/**";
        return matcher.extractUriTemplateVariables(pattern, path).get(TENANT_NAME);
    }

    private boolean isUnderTenantFolder(String path) {
        final String pattern = TENANT_PREFIX + appName + "/lep/**";
        return matcher.match(pattern, path);
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }
}
