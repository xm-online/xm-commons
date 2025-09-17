package com.icthh.xm.commons.permission.service.custom;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toList;

public interface CustomPrivilegesExtractor<S extends ConfigWithKey> {

    String getSectionName();

    default List<Map<String, Object>> toPrivileges(Collection<S> specs) {
        return toPrivilegesList(specs).stream()
            .map(this::toPrivilege)
            .collect(toList());
    }

    List<String> toPrivilegesList(Collection<S> specs);

    default Map<String, Object> toPrivilege(String key) {
        return of("key", getPrivilegePrefix() + key);
    }

    default String getPrivilegePrefix() {
        return StringUtils.EMPTY;
    }

    default boolean isEnabled(String tenantKey) {
        return true;
    }
}
