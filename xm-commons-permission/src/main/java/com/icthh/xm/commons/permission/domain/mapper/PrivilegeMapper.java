package com.icthh.xm.commons.permission.domain.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.permission.domain.Privilege;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@UtilityClass
@Slf4j
public class PrivilegeMapper {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Convert privileges collection to yml string.
     *
     * @param privileges collection
     * @return yml string
     */
    public String privilegesToYml(Collection<Privilege> privileges) {
        try {
            Map<String, Set<Privilege>> map = new TreeMap<>();
            privileges.forEach(privilege -> {
                map.putIfAbsent(privilege.getMsName(), new TreeSet<>());
                map.get(privilege.getMsName()).add(privilege);
            });
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to create privileges YML file from collection, error: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Convert privileges map to yml string.
     *
     * @param privileges map
     * @return yml string
     */
    public String privilegesMapToYml(Map<String, Collection<Privilege>> privileges) {
        try {
            return mapper.writeValueAsString(privileges);
        } catch (Exception e) {
            log.error("Failed to create privileges YML file from map, error: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Convert privileges yml string to map.
     *
     * @param yml string
     * @return privileges map
     */
    public Map<String, Set<Privilege>> ymlToPrivileges(String yml) {
        try {
            Map<String, Set<Privilege>> map = mapper.readValue(yml,
                new TypeReference<TreeMap<String, TreeSet<Privilege>>>() {
                });
            map.forEach((msName, privileges) -> privileges.forEach(privilege -> privilege.setMsName(msName)));
            return Collections.unmodifiableMap(map);
        } catch (Exception e) {
            log.error("Failed to create privileges collection from YML file, error: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
