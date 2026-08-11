package com.icthh.xm.commons.config.client.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds per-tenant {@code order.yml} specifications and sorts configuration paths so that files are
 * delivered to {@link com.icthh.xm.commons.config.client.api.RefreshableConfiguration} beans in the
 * order defined by the tenant: the {@code order.yml} file itself first, then files by first matching
 * Ant pattern (patterns are relative to the tenant root), then unmatched files in their original order.
 * Paths of tenants without {@code order.yml} and non-tenant paths keep their positions.
 */
@Slf4j
public class ConfigurationOrderService {

    private static final String ORDER_FILE_NAME = "order.yml";
    private static final String ORDER_CONFIG_PATTERN = "/config/tenants/{tenantName}/" + ORDER_FILE_NAME;
    private static final String TENANT_FILE_PATTERN = "/config/tenants/{tenantName}/**";
    private static final String TENANT_NAME = "tenantName";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, List<String>> orderByTenant = new ConcurrentHashMap<>();

    public void processOrderConfigs(Map<String, Configuration> configs) {
        configs.forEach((path, configuration) -> processOrderEntryIfMatches(path, configuration));
    }

    /**
     * Same as {@link #processOrderConfigs(Map)}, but scans the collection of dispatched paths rather
     * than the fetched map's keys. This matters for deletions: in production
     * ({@code XmMsConfigCommonConfigRepository}) a deleted path is dispatched (present in {@code paths})
     * but absent from the fetched map, so scanning the map alone would never observe the deletion and
     * a stale order would persist. Looking the path up in {@code configs} (which may yield {@code null})
     * lets the existing null/blank-content handling in {@link #updateOrder} remove the tenant's entry.
     */
    public void processOrderConfigs(Collection<String> paths, Map<String, Configuration> configs) {
        paths.forEach(path -> processOrderEntryIfMatches(path, configs.get(path)));
    }

    private void processOrderEntryIfMatches(String path, Configuration configuration) {
        if (path != null && matcher.match(ORDER_CONFIG_PATTERN, path)) {
            updateOrder(path, configuration != null ? configuration.getContent() : null);
        }
    }

    public List<String> sortPaths(Collection<String> paths) {
        try {
            return doSort(paths);
        } catch (Exception e) {
            log.error("Error during sorting configuration paths, original order kept", e);
            return new ArrayList<>(paths);
        }
    }

    private List<String> doSort(Collection<String> paths) {
        List<String> result = new ArrayList<>(paths);
        Map<String, List<String>> orders = Map.copyOf(orderByTenant);
        if (orders.isEmpty()) {
            return result;
        }

        Map<String, List<Integer>> tenantIndexes = new LinkedHashMap<>();
        for (int i = 0; i < result.size(); i++) {
            String tenant = getTenant(result.get(i));
            if (tenant != null && orders.containsKey(tenant)) {
                tenantIndexes.computeIfAbsent(tenant, t -> new ArrayList<>()).add(i);
            }
        }

        tenantIndexes.forEach((tenant, indexes) -> {
            List<String> patterns = orders.get(tenant);
            Map<String, Integer> ranks = new HashMap<>();
            indexes.forEach(i -> ranks.computeIfAbsent(result.get(i), p -> rank(tenant, p, patterns)));
            List<String> sorted = indexes.stream()
                .map(result::get)
                .sorted(Comparator.comparingInt(ranks::get))
                .toList();
            for (int i = 0; i < indexes.size(); i++) {
                result.set(indexes.get(i), sorted.get(i));
            }
        });
        return result;
    }

    private int rank(String tenant, String path, List<String> patterns) {
        String relativePath = getRelativePath(tenant, path);
        if (ORDER_FILE_NAME.equals(relativePath)) {
            return -1;
        }
        for (int i = 0; i < patterns.size(); i++) {
            if (matcher.match(patterns.get(i), relativePath)) {
                return i;
            }
        }
        return patterns.size();
    }

    private String getTenant(String path) {
        if (path != null && matcher.match(TENANT_FILE_PATTERN, path)) {
            return matcher.extractUriTemplateVariables(TENANT_FILE_PATTERN, path).get(TENANT_NAME);
        }
        return null;
    }

    private String getRelativePath(String tenant, String path) {
        String prefix = "/config/tenants/" + tenant + "/";
        if (path.length() <= prefix.length()) {
            return "";
        }
        return path.substring(prefix.length());
    }

    private void updateOrder(String path, String content) {
        String tenant = matcher.extractUriTemplateVariables(ORDER_CONFIG_PATTERN, path).get(TENANT_NAME);
        if (StringUtils.isBlank(content)) {
            orderByTenant.remove(tenant);
            log.info("Order configuration removed for tenant [{}]", tenant);
            return;
        }
        try {
            OrderSpec spec = mapper.readValue(content, OrderSpec.class);
            List<String> order = spec == null || spec.getOrder() == null ? List.of() :
                spec.getOrder().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(pattern -> StringUtils.removeStart(pattern.trim(), "/"))
                    .toList();
            orderByTenant.put(tenant, order);
            log.info("Order configuration updated for tenant [{}] with {} patterns", tenant, order.size());
        } catch (Exception e) {
            log.error("Error parsing order configuration [{}], previous order kept", path, e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderSpec {
        private List<String> order;
    }
}
