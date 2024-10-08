package com.icthh.xm.commons.config.client.api.refreshable;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.config.client.utils.Utils.nullSafeList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public abstract class MapRefreshableConfiguration<CONFIG_ITEM extends ConfigWithKey, CONFIG_FILE>
    extends SimpleConfigItemClassRefreshableConfiguration<Map<String, CONFIG_ITEM>, CONFIG_FILE> {

    public MapRefreshableConfiguration(@Value("${spring.application.name}") String appName,
                                       TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    public final Map<String, CONFIG_ITEM> joinTenantConfiguration(List<CONFIG_FILE> files) {
        Map<String, CONFIG_ITEM> map = new HashMap<>();
        files.stream().map(this::toConfigItems)
            .map(nullSafeList())
            .flatMap(List::stream)
            .forEach(it -> map.put(it.getKey(), it)); //collect throw exception on key duplication
        return map;
    }

    @Override
    protected Map<String, CONFIG_ITEM> getConfiguration(String tenantKey) {
        Map<String, CONFIG_ITEM> configuration = super.getConfiguration(tenantKey);
        return configuration == null ? Map.of() : configuration;
    }

    protected abstract List<CONFIG_ITEM> toConfigItems(CONFIG_FILE config);
}
