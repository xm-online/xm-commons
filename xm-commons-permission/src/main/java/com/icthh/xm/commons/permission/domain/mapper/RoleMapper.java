package com.icthh.xm.commons.permission.domain.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.permission.domain.Role;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@UtilityClass
@Slf4j
public class RoleMapper {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Convert roles collection to yml string.
     *
     * @param roles collection
     * @return yml string
     */
    public String rolesToYml(Collection<Role> roles) {
        try {
            Map<String, Role> map = new TreeMap<>();
            roles.forEach(role -> map.put(role.getKey(), role));
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to create roles YML file from collection, error: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Convert roles yml string to map.
     *
     * @param yml string
     * @return roles map
     */
    public Map<String, Role> ymlToRoles(String yml) {
        try {
            Map<String, Role> map = mapper
                .readValue(yml, new TypeReference<TreeMap<String, Role>>() {
                });
            map.forEach((roleKey, role) -> role.setKey(roleKey));
            return map;
        } catch (Exception e) {
            log.error("Failed to create roles collection from YML file, error: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
