package com.icthh.xm.commons.listener;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class JsonListenerService {

    private final Map<String, Map<String, String>> tenantsSpecificationsByPath = new LinkedHashMap<>();

    public void processTenantSpecification(String tenantName, String relativePath, String config) {
        Map<String, String> relativePathSpecMap = tenantsSpecificationsByPath
            .computeIfAbsent(tenantName, t -> new ConcurrentHashMap<>());

        if (isBlank(config)) {
            relativePathSpecMap.remove(relativePath);
        } else {
            relativePathSpecMap.put(relativePath, config);
        }
    }

    public String getSpecificationByTenantRelativePath(String tenant, String relativePath) {
        if (relativePath == null) {
            return EMPTY;
        }
        return ofNullable(getSpecificationByTenant(tenant))
            .map(spec -> spec.get(relativePath))
            .orElse(EMPTY);
    }

    public Map<String, String> getSpecificationByTenant(String tenant) {
        return tenantsSpecificationsByPath.get(tenant);
    }
}
