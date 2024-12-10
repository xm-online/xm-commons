package com.icthh.xm.commons.listener;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;

import static com.icthh.xm.commons.utils.DataSpecConstants.TENANT_NAME;

public abstract class AbstractJsonConfigurationListener implements RefreshableConfiguration {

    private final String mappingPath;
    private final AntPathMatcher matcher;
    private final JsonListenerService jsonListenerService;

    public AbstractJsonConfigurationListener(String mappingPath,
                                             JsonListenerService jsonListenerService) {
        this.jsonListenerService = jsonListenerService;
        this.mappingPath = mappingPath;
        this.matcher = new AntPathMatcher();
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String tenantName = extractTenantName(updatedKey);
        String relativePath = updatedKey.substring(updatedKey.indexOf(getSpecificationKey()));
        jsonListenerService.processTenantSpecification(tenantName, relativePath, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(mappingPath, updatedKey);
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        paths.stream().map(this::extractTenantName).forEach(this::updateByTenantState);
    }

    private String extractTenantName(String updatedKey) {
        return matcher.extractUriTemplateVariables(mappingPath, updatedKey).get(TENANT_NAME);
    }

    public abstract String getSpecificationKey();
    public abstract void updateByTenantState(String tenant);
}
