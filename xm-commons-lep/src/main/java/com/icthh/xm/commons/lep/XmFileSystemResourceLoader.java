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
    private static final String TENANT_PLACEHOLDER_NAME = "tenantName";
    private static final String TENANT_PLACEHOLDER = "{" + TENANT_PLACEHOLDER_NAME + "}";
    private static final String TENANT_PREFIX = "/config/tenants/";

    private final FileSystemResourceLoader delegate;
    private final TenantAliasService tenantAliasService;
    private final String appName;

    @Override
    public Resource getResource(String location) {
        Resource resource = delegate.getResource(location);
        if (resource.exists() || !(isUnderTenantFolder(location))) {
            return resource;
        }

        return tenantAliasService.getTenantAliasTree().getParents(extractTenantName(location))
                .stream()
                .map(TenantAliasTree.TenantAlias::getKey)
                .map(tenantKey -> getPathInTenant(location, tenantKey))
                .map(delegate::getResource)
                .filter(Resource::exists)
                .findFirst()
                .orElse(resource);
    }

    public String getPathInTenant(String path, String targetTenant) {
        return getTenantName(path)
                .map(sourceTenant -> replaceTenantName(path, sourceTenant, targetTenant))
                .orElse(path);
    }

    public Optional<String> getTenantName(String path) {
        return Optional.of(path).filter(this::isUnderTenantFolder).map(this::extractTenantName);
    }

    private String replaceTenantName(String path, String sourceTenant, String targetTenant) {
        return path.replace(getLepPath(sourceTenant), getLepPath(targetTenant));
    }

    private String extractTenantName(String path) {
        return matcher.extractUriTemplateVariables(lepPathPattern(), path).get(TENANT_PLACEHOLDER_NAME);
    }

    private String lepPathPattern() {
        return "**" + getLepPath(TENANT_PLACEHOLDER) + "**";
    }

    private String getLepPath(String tenant) {
        return TENANT_PREFIX + tenant + "/" + appName + "/lep/";
    }

    private boolean isUnderTenantFolder(String path) {
        return matcher.match(lepPathPattern(), path);
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }
}
