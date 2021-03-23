package com.icthh.xm.commons.permission.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.service.filter.PermissionMsNameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionMappingService {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final PermissionMsNameFilter permissionMsNameFilter;

    @LoggingAspectConfig(inputDetails = false, resultDetails = false)
    public Map<String, Permission> ymlToPermissions(String yml) {
        Map<String, Permission> result = new TreeMap<>();
        try {
            Map<String, Map<String, Set<Permission>>> permissionMap = deserializeYml(yml);
            permissionMap.entrySet().stream()
                .filter(entry -> permissionMsNameFilter.filterPermission(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> entry.getValue()
                    .forEach((roleKey, permissions) ->
                        permissions.forEach(permission -> {
                            permission.setMsName(entry.getKey());
                            permission.setRoleKey(roleKey);
                            result.put(roleKey + ":" + permission.getPrivilegeKey(), permission);
                        })
                    ));
        } catch (Exception e) {
            log.error("Failed to create permissions collection from YML file, error: {}", e.getMessage(), e);
        }
        return Collections.unmodifiableMap(result);
    }

    @LoggingAspectConfig(inputDetails = false, resultDetails = false)
    public List<Permission> ymlToPermissionsList(String yml) {
        List<Permission> result = new ArrayList<>();
        try {
            Map<String, Map<String, Set<Permission>>> map = deserializeYml(yml);
            map.entrySet().stream()
                .filter(entry -> permissionMsNameFilter.filterPermission(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> entry.getValue()
                    .forEach((roleKey, permissions) ->
                        permissions.forEach(permission -> {
                            permission.setMsName(entry.getKey());
                            permission.setRoleKey(roleKey);
                            result.add(permission);
                        })
                    ));

        } catch (Exception e) {
            log.error("Failed to create permissions collection from YML file, error: {}", e.getMessage(), e);
        }
        return Collections.unmodifiableList(result);
    }

    private Map<String, Map<String, Set<Permission>>> deserializeYml(String yml) throws java.io.IOException {
        return mapper.readValue(yml, new TypeReference<TreeMap<String, TreeMap<String, TreeSet<Permission>>>>() {
        });
    }
}
