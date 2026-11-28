# `order.yml` Ordered Configuration Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A tenant-root `order.yml` file (`/config/tenants/{TENANT}/order.yml`) controls the order in which configuration files are delivered to `RefreshableConfiguration` beans, on startup and on config-changed events, for both `onRefresh`/`onInit` and `refreshFinished`.

**Architecture:** One new stateful service, `ConfigurationOrderService`, caches parsed per-tenant `order.yml` patterns and sorts path lists. It is hooked into the two delivery flows: `AbstractConfigService.updateConfigurations` (events) and `InitRefreshableConfigurationBeanPostProcessor` (startup). Spec: `docs/superpowers/specs/2026-07-06-config-order-yml-design.md`.

**Tech Stack:** Java 25, Gradle multi-module build, Spring (no Boot autoconfig magic — explicit `@Bean`s in `XmConfigConfiguration`), Jackson 2 YAML (`com.fasterxml.jackson.dataformat.yaml.YAMLFactory`), Lombok, JUnit 4 + Mockito.

## Global Constraints

- Repo root: `/Users/serhii.senko/work/XM/xm-commons`. All commands run from there. Module: `xm-commons-config`.
- Work on branch `feature/config-order-yml` (create it before Task 1 if it does not exist: `git checkout -b feature/config-order-yml`).
- Tests are JUnit 4 (`org.junit.Test`) with `MockitoJUnitRunner` — do NOT use JUnit 5 imports.
- YAML parsing uses Jackson 2 (`com.fasterxml.jackson.databind.ObjectMapper` + `YAMLFactory`) — the module already depends on `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`. Do NOT add new dependencies. Do NOT use the Jackson 3 (`tools.jackson.*`) imports for this feature.
- Ant patterns matched with `org.springframework.util.AntPathMatcher` (already on classpath).
- A broken `order.yml` must never break configuration refresh: parse errors keep the previous cached order; sort errors fall back to the original list order.
- Constructor signature changes to `AbstractConfigService`, `CommonConfigService`, `InitRefreshableConfigurationBeanPostProcessor` are intended (spec-approved breaking change). Every in-repo call site is listed in the tasks below — there are no others (verified by grep).
- Commit after every task. End git commit messages with the standard trailer used in this session.

---

### Task 1: `ConfigurationOrderService` (cache + sorting core)

**Files:**
- Create: `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/service/ConfigurationOrderService.java`
- Test: `xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/service/ConfigurationOrderServiceUnitTest.java`

**Interfaces:**
- Consumes: `com.icthh.xm.commons.config.domain.Configuration` (existing domain class with `getPath()`/`getContent()`).
- Produces (used by Tasks 2 and 3):
  - `public ConfigurationOrderService()` — no-arg constructor.
  - `public void processOrderConfigs(Map<String, Configuration> configs)` — scans map keys for `/config/tenants/{tenantName}/order.yml`, parses content, updates cache; `null` configuration or blank content removes the tenant entry.
  - `public List<String> sortPaths(Collection<String> paths)` — returns a NEW sorted list; never mutates input; never throws.

- [ ] **Step 1: Write the failing test**

Create `xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/service/ConfigurationOrderServiceUnitTest.java`:

```java
package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.domain.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigurationOrderServiceUnitTest {

    private static final String XM_ORDER_PATH = "/config/tenants/XM/order.yml";
    private static final String XM_ORDER_CONTENT = """
        order:
          - tenant-config.yml
          - entity/xmentityspec/*.yml
          - lep/**
        """;

    private ConfigurationOrderService service;

    @Before
    public void setUp() {
        service = new ConfigurationOrderService();
    }

    private void seedXmOrder() {
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, XM_ORDER_CONTENT)));
    }

    @Test
    public void keepsOriginalOrderWhenNoOrderConfig() {
        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void sortsByPatternsWithOrderFileFirstAndUnmatchedLast() {
        seedXmOrder();
        List<String> paths = List.of(
            "/config/tenants/XM/lep/some/Script$$around.groovy",
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/entity/xmentityspec/specs.yml",
            XM_ORDER_PATH,
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            XM_ORDER_PATH,
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/entity/xmentityspec/specs.yml",
            "/config/tenants/XM/lep/some/Script$$around.groovy",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void sortIsStableWithinSameRank() {
        seedXmOrder();
        List<String> paths = List.of(
            "/config/tenants/XM/lep/b/Second.groovy",
            "/config/tenants/XM/lep/a/First.groovy",
            "/config/tenants/XM/webapp/z.yml",
            "/config/tenants/XM/webapp/a.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void keepsOtherTenantsAndNonTenantPathsInPlace() {
        seedXmOrder();
        List<String> paths = List.of(
            "/config/tenants/TEST/a.yml",
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/TEST/b.yml",
            "/config/tenants/XM/tenant-config.yml",
            "/some/other/path.yml");

        List<String> expected = List.of(
            "/config/tenants/TEST/a.yml",
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/TEST/b.yml",
            "/config/tenants/XM/webapp/settings-public.yml",
            "/some/other/path.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void invalidYamlKeepsPreviousOrder() {
        seedXmOrder();
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, "{{{ not valid yaml")));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void deletedOrderFileRemovesOrdering() {
        seedXmOrder();
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, null)));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        assertEquals(paths, service.sortPaths(paths));
    }

    @Test
    public void ignoresUnknownTopLevelKeys() {
        String content = """
            someFutureKey: true
            order:
              - tenant-config.yml
            """;
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, content)));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void normalizesLeadingSlashInPatterns() {
        String content = "order:\n  - /tenant-config.yml\n";
        service.processOrderConfigs(Map.of(XM_ORDER_PATH, new Configuration(XM_ORDER_PATH, content)));

        List<String> paths = List.of(
            "/config/tenants/XM/webapp/settings-public.yml",
            "/config/tenants/XM/tenant-config.yml");

        List<String> expected = List.of(
            "/config/tenants/XM/tenant-config.yml",
            "/config/tenants/XM/webapp/settings-public.yml");

        assertEquals(expected, service.sortPaths(paths));
    }

    @Test
    public void commonsIsTreatedLikeAnyTenant() {
        String commonsOrderPath = "/config/tenants/commons/order.yml";
        service.processOrderConfigs(Map.of(commonsOrderPath,
            new Configuration(commonsOrderPath, "order:\n  - lep/**\n")));

        List<String> paths = List.of(
            "/config/tenants/commons/other.yml",
            "/config/tenants/commons/lep/Script$$tenant.groovy");

        List<String> expected = List.of(
            "/config/tenants/commons/lep/Script$$tenant.groovy",
            "/config/tenants/commons/other.yml");

        assertEquals(expected, service.sortPaths(paths));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :xm-commons-config:test --tests "com.icthh.xm.commons.config.client.service.ConfigurationOrderServiceUnitTest"`
Expected: FAIL — compilation error: `cannot find symbol: class ConfigurationOrderService`

- [ ] **Step 3: Write the implementation**

Create `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/service/ConfigurationOrderService.java`:

```java
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
        configs.forEach((path, configuration) -> {
            if (path != null && matcher.match(ORDER_CONFIG_PATTERN, path)) {
                updateOrder(path, configuration != null ? configuration.getContent() : null);
            }
        });
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
            List<String> sorted = indexes.stream()
                .map(result::get)
                .sorted(Comparator.comparingInt(path -> rank(tenant, path, orders.get(tenant))))
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
        return path.substring(("/config/tenants/" + tenant + "/").length());
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
```

Implementation notes (constraints the code must keep):
- `doSort` mutates only its local copy; the `catch` in `sortPaths` returns a FRESH `new ArrayList<>(paths)` so a mid-sort failure cannot leak a partially permuted list.
- `Map.copyOf(orderByTenant)` snapshots the cache once per sort — the Kafka consumer thread may update `orderByTenant` concurrently.
- `Stream.sorted` is stable, which provides "unmatched files keep original relative order" and stability inside each pattern group.
- Tenant files are written back into the SAME index slots they occupied, which implements "cross-tenant positions untouched".

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :xm-commons-config:test --tests "com.icthh.xm.commons.config.client.service.ConfigurationOrderServiceUnitTest"`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
git add xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/service/ConfigurationOrderService.java \
        xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/service/ConfigurationOrderServiceUnitTest.java \
        docs/superpowers/specs/2026-07-06-config-order-yml-design.md \
        docs/superpowers/plans/2026-07-06-config-order-yml.md
git commit -m "feat(config): add ConfigurationOrderService for order.yml based path ordering"
```

---

### Task 2: Event flow — order dispatch in `AbstractConfigService.updateConfigurations`

**Files:**
- Modify: `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/api/AbstractConfigService.java`
- Modify: `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/service/CommonConfigService.java`
- Modify: `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/config/XmConfigConfiguration.java` (lines 61-66, `configService` bean; add `configurationOrderService` bean)
- Modify: `xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/service/CommonConfigServiceUnitTest.java` (constructor call sites at lines 39, 71, 86 + new tests)
- Modify: `xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/config/InitRefreshableConfigurationBeanPostProcessorUnitTest.java` (line 58 only — `new CommonConfigService(...)` gains the third argument; needed to keep the module compiling)

**Interfaces:**
- Consumes (from Task 1): `ConfigurationOrderService`, `processOrderConfigs(Map<String, Configuration>)`, `sortPaths(Collection<String>)`.
- Produces (relied on by Task 3 and downstream code):
  - `protected AbstractConfigService(FetchConfigurationSettings fetchConfigurationSettings, ConfigurationOrderService configurationOrderService)`
  - `public CommonConfigService(FetchConfigurationSettings fetchConfigurationSettings, CommonConfigRepository commonConfigRepository, ConfigurationOrderService configurationOrderService)`
  - `@Bean public ConfigurationOrderService configurationOrderService()` in `XmConfigConfiguration`.
  - Behavior: `updateConfigurations` dispatches `onConfigurationChanged` in sorted order and calls `refreshFinished` with the sorted filtered path list.

- [ ] **Step 1: Write the failing tests**

In `CommonConfigServiceUnitTest.java` add imports:

```java
import static org.mockito.Mockito.inOrder;

import com.icthh.xm.commons.config.domain.Configuration;
import org.mockito.InOrder;
```

(`Configuration` is already imported; keep existing imports intact.)

Add two test methods at the end of the class:

```java
    @Test
    public void updateConfigurationsDispatchesInOrderYmlOrder() {
        String orderPath = "/config/tenants/XM/order.yml";
        String orderContent = "order:\n  - tenant-config.yml\n  - entity/**\n";
        String tenantConfigPath = "/config/tenants/XM/tenant-config.yml";
        String entityPath = "/config/tenants/XM/entity/specs.yml";
        String otherPath = "/config/tenants/XM/webapp/settings.yml";

        List<String> incoming = List.of(otherPath, entityPath, tenantConfigPath, orderPath);
        Map<String, Configuration> config = Map.of(
            orderPath, new Configuration(orderPath, orderContent),
            tenantConfigPath, new Configuration(tenantConfigPath, "c1"),
            entityPath, new Configuration(entityPath, "c2"),
            otherPath, new Configuration(otherPath, "c3"));
        when(commonConfigRepository.getConfig(eq("commit"), anyList())).thenReturn(config);

        ConfigurationChangedListener listener = mock(ConfigurationChangedListener.class);
        configService.addConfigurationChangedListener(listener);

        configService.updateConfigurations("commit", incoming);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(orderPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(tenantConfigPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(entityPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(config.get(otherPath)));
        verify(listener).refreshFinished(List.of(orderPath, tenantConfigPath, entityPath, otherPath));
    }

    @Test
    public void orderFromPreviousBatchAppliesToNextBatch() {
        String orderPath = "/config/tenants/XM/order.yml";
        Map<String, Configuration> firstBatch = Map.of(orderPath,
            new Configuration(orderPath, "order:\n  - tenant-config.yml\n"));
        when(commonConfigRepository.getConfig(eq("commit1"), anyList())).thenReturn(firstBatch);
        configService.updateConfigurations("commit1", List.of(orderPath));

        String tenantConfigPath = "/config/tenants/XM/tenant-config.yml";
        String otherPath = "/config/tenants/XM/webapp/settings.yml";
        Map<String, Configuration> secondBatch = Map.of(
            tenantConfigPath, new Configuration(tenantConfigPath, "c1"),
            otherPath, new Configuration(otherPath, "c2"));
        when(commonConfigRepository.getConfig(eq("commit2"), anyList())).thenReturn(secondBatch);

        ConfigurationChangedListener listener = mock(ConfigurationChangedListener.class);
        configService.addConfigurationChangedListener(listener);
        configService.updateConfigurations("commit2", List.of(otherPath, tenantConfigPath));

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onConfigurationChanged(refEq(secondBatch.get(tenantConfigPath)));
        inOrder.verify(listener).onConfigurationChanged(refEq(secondBatch.get(otherPath)));
        verify(listener).refreshFinished(List.of(tenantConfigPath, otherPath));
    }
```

Also update the three construction sites in this test file to pass the new third argument (this is required for the test file to compile once the production constructor changes in Step 3; adding it now is fine — it fails compilation first, which is our RED):

Line 39: `configService = new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService());`
Line 71: `configService = spy(new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService()));`
Line 86: `CommonConfigService configService = spy(new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService()));`

Add the import: `import com.icthh.xm.commons.config.client.service.ConfigurationOrderService;` — NOT needed (same package `com.icthh.xm.commons.config.client.service`); skip the import.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :xm-commons-config:test --tests "com.icthh.xm.commons.config.client.service.CommonConfigServiceUnitTest"`
Expected: FAIL — compilation error: constructor `CommonConfigService` cannot be applied to given types (3 args passed, 2 expected)

- [ ] **Step 3: Implement production changes**

**3a.** `AbstractConfigService.java` — add import, field, constructor param, and sorting in `updateConfigurations`:

```java
import com.icthh.xm.commons.config.client.service.ConfigurationOrderService;
```

Replace the fields + constructor block:

```java
    private final List<ConfigurationChangedListener> configurationListeners = new ArrayList<>();
    private final AntPathMatcher antPathMatcher;
    private final FetchConfigurationSettings fetchConfigurationSettings;
    private final ConfigurationOrderService configurationOrderService;

    protected AbstractConfigService(FetchConfigurationSettings fetchConfigurationSettings,
                                    ConfigurationOrderService configurationOrderService) {
        this.antPathMatcher = new AntPathMatcher();
        this.fetchConfigurationSettings = fetchConfigurationSettings;
        this.configurationOrderService = configurationOrderService;
    }
```

Replace `updateConfigurations`:

```java
    /**
     * Update configuration from config service.
     * Paths are dispatched in the order defined by the tenant's order.yml (if any):
     * order.yml itself first, then files by first matching pattern, then the rest.
     *
     * @param commit commit hash, will be empty if configuration deleted
     * @param paths collection of paths updated
     */
    @Override
    public void updateConfigurations(String commit, Collection<String> paths) {
        final Collection<String> filteredPaths = getFilteredPaths(paths);
        if (!filteredPaths.isEmpty()) {
            Map<String, Configuration> configurationsMap = getConfigurationMap(commit, filteredPaths);
            configurationOrderService.processOrderConfigs(configurationsMap);
            List<String> sortedPaths = configurationOrderService.sortPaths(paths);
            List<String> sortedFilteredPaths = configurationOrderService.sortPaths(filteredPaths);
            sortedPaths.forEach(path -> notifyUpdated(getNonNullConfiguration(configurationsMap, path)));
            configurationListeners.forEach(it -> it.refreshFinished(sortedFilteredPaths));
        }
    }
```

**3b.** `CommonConfigService.java` — constructor:

```java
    public CommonConfigService(FetchConfigurationSettings fetchConfigurationSettings,
                               CommonConfigRepository commonConfigRepository,
                               ConfigurationOrderService configurationOrderService) {
        super(fetchConfigurationSettings, configurationOrderService);
        this.commonConfigRepository = commonConfigRepository;
    }
```

(No import needed — same package.)

**3c.** `XmConfigConfiguration.java` — add import `com.icthh.xm.commons.config.client.service.ConfigurationOrderService;`, add the new bean, and rewire `configService`:

```java
    @Bean
    public ConfigurationOrderService configurationOrderService() {
        return new ConfigurationOrderService();
    }

    @Bean
    public ConfigService configService(
        CommonConfigRepository commonConfigRepository,
        FetchConfigurationSettings fetchConfigurationSettings,
        ConfigurationOrderService configurationOrderService) {
        return new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, configurationOrderService);
    }
```

**3d.** `InitRefreshableConfigurationBeanPostProcessorUnitTest.java` line 58 — keep module compiling:

```java
        configService = new CommonConfigService(fetchConfigurationSettings, commonConfigRepository, new ConfigurationOrderService());
```

Add import: `import com.icthh.xm.commons.config.client.service.ConfigurationOrderService;`

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :xm-commons-config:test --tests "com.icthh.xm.commons.config.client.service.CommonConfigServiceUnitTest" --tests "com.icthh.xm.commons.config.client.config.InitRefreshableConfigurationBeanPostProcessorUnitTest"`
Expected: PASS — all existing tests still green (existing tests exercise an empty order cache, which sorts to the original order), plus the 2 new tests

- [ ] **Step 5: Commit**

```bash
git add xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/api/AbstractConfigService.java \
        xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/service/CommonConfigService.java \
        xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/config/XmConfigConfiguration.java \
        xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/service/CommonConfigServiceUnitTest.java \
        xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/config/InitRefreshableConfigurationBeanPostProcessorUnitTest.java
git commit -m "feat(config): apply order.yml ordering to config update events"
```

---

### Task 3: Startup flow — ordered init in `InitRefreshableConfigurationBeanPostProcessor`

**Files:**
- Modify: `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/config/InitRefreshableConfigurationBeanPostProcessor.java`
- Modify: `xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/config/XmConfigConfiguration.java` (lines 68-72, `refreshableConfigurationPostProcessor` bean)
- Modify: `xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/config/InitRefreshableConfigurationBeanPostProcessorUnitTest.java` (5 constructor call sites at lines 89, 104, 116, 133, 161 + new test)

**Interfaces:**
- Consumes (from Tasks 1-2): `ConfigurationOrderService` bean; `processOrderConfigs`; `sortPaths`.
- Produces:
  - `public InitRefreshableConfigurationBeanPostProcessor(ObjectProvider<ConfigService> configServiceProvider, XmConfigProperties xmConfigProperties, FetchConfigurationSettings fetchConfigurationSettings, ConfigurationOrderService configurationOrderService)`
  - Behavior: startup `onInit` calls happen in order.yml order; the initial fetch seeds the shared order cache; per-bean `refreshFinished` receives the ordered path list.

- [ ] **Step 1: Write the failing test**

In `InitRefreshableConfigurationBeanPostProcessorUnitTest.java`:

Add imports:

```java
import static org.mockito.Mockito.inOrder;

import com.icthh.xm.commons.config.client.service.ConfigurationOrderService;
import org.mockito.InOrder;

import java.util.LinkedHashMap;
```

(`ConfigurationOrderService` import was already added in Task 2 — do not duplicate it.)

Add a field next to the other fields:

```java
    private final ConfigurationOrderService configurationOrderService = new ConfigurationOrderService();
```

Update the FIVE processor constructions (lines 89, 104, 116, 133, 161) to pass it as the 4th argument, e.g.:

```java
        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings, configurationOrderService);
```

Add the new test at the end of the class:

```java
    @Test
    public void shouldInitInOrderDefinedByOrderYml() {
        String orderPath = "/config/tenants/TENANT1/order.yml";
        String uaaPath = "/config/tenants/TENANT1/uaa/uaa.yml";
        String dashboardPath = "/config/tenants/TENANT1/dashboard/dashboards/ADMIN_METRICS-27.yml";

        Map<String, Configuration> orderedConfigMap = new LinkedHashMap<>();
        orderedConfigMap.put(uaaPath, Configuration.of().build());
        orderedConfigMap.put(dashboardPath, Configuration.of().build());
        orderedConfigMap.put(orderPath, Configuration.of().content("order:\n  - uaa/**\n").build());

        when(configService.getConfigMapAntPattern(any(), any())).thenReturn(orderedConfigMap);
        when(configServiceProvider.getIfAvailable()).thenReturn(configService);

        processor = new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, configProperties, fetchConfigurationSettings, configurationOrderService);
        processor.postProcessBeforeInitialization(refreshableConfiguration, "refreshableConfiguration");
        processor.postProcessAfterInitialization(refreshableConfiguration, "refreshableConfiguration");

        InOrder order = inOrder(refreshableConfiguration);
        order.verify(refreshableConfiguration).onInit(eq(orderPath), any());
        order.verify(refreshableConfiguration).onInit(eq(uaaPath), any());
        order.verify(refreshableConfiguration).onInit(eq(dashboardPath), any());
        verify(refreshableConfiguration).refreshFinished(List.of(orderPath, uaaPath, dashboardPath));
    }
```

(The `when(configService.getConfigMapAntPattern(...))` line follows the exact pattern of the existing `shouldContainAllTenantsIfIncludePropertyEmptyDuringUpdate` test — it stubs the underlying mocked `commonConfigRepository` call through the real `CommonConfigService`.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :xm-commons-config:test --tests "com.icthh.xm.commons.config.client.config.InitRefreshableConfigurationBeanPostProcessorUnitTest"`
Expected: FAIL — compilation error: constructor `InitRefreshableConfigurationBeanPostProcessor` cannot be applied to given types (4 args passed, 3 expected)

- [ ] **Step 3: Implement production changes**

**3a.** `InitRefreshableConfigurationBeanPostProcessor.java`:

Add import:

```java
import com.icthh.xm.commons.config.client.service.ConfigurationOrderService;
```

Add field and constructor parameter:

```java
    private final Set<String> includedTenants;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final FetchConfigurationSettings fetchConfigurationSettings;
    private final ConfigurationOrderService configurationOrderService;

    public InitRefreshableConfigurationBeanPostProcessor(ObjectProvider<ConfigService> configServiceProvider,
                                                         XmConfigProperties xmConfigProperties,
                                                         FetchConfigurationSettings fetchConfigurationSettings,
                                                         ConfigurationOrderService configurationOrderService) {
        this.configServiceProvider = configServiceProvider;
        this.includedTenants = xmConfigProperties.getIncludeTenantUppercase();
        this.fetchConfigurationSettings = fetchConfigurationSettings;
        this.configurationOrderService = configurationOrderService;
        addLepCommons();
    }
```

Seed the order cache from the initial fetch in `getConfig()`:

```java
    private Map<String, Configuration> getConfig() {
        if (configMap == null) {
            configMap = getConfigService().getConfigMapAntPattern(null, fetchConfigurationSettings.getMsConfigPatterns());
            configurationOrderService.processOrderConfigs(configMap);
        }
        return configMap;
    }
```

Replace `initConfigPaths` and `printLog` (the stream now walks sorted keys instead of map entries, so `printLog` takes the key + configuration):

```java
    public List<String> initConfigPaths(final RefreshableConfiguration refreshableConfiguration,
                                        final Map<String, Configuration> configMap) {
        return configurationOrderService.sortPaths(configMap.keySet())
            .stream()
            .filter(this::isTenantIncluded)
            .filter(refreshableConfiguration::isListeningConfiguration)
            .peek(key -> printLog(getBeanName(refreshableConfiguration), key, configMap.get(key)))
            .peek(key -> refreshableConfiguration.onInit(key, configMap.get(key).getContent()))
            .collect(Collectors.toList());
    }

    private static void printLog(final String beanName, final String key, final Configuration configuration) {
        log.info("Process config init event: [key = {}, size = {}, newHash = {}] in bean: [{}]",
                 key,
                 length(configuration.getContent()),
                 getValueHash(configuration.getContent()),
                 beanName);
    }
```

**3b.** `XmConfigConfiguration.java` — rewire the post-processor bean:

```java
    @Bean
    public InitRefreshableConfigurationBeanPostProcessor refreshableConfigurationPostProcessor(
            ObjectProvider<ConfigService> configServiceProvider, XmConfigProperties xmConfigProperties,
            FetchConfigurationSettings fetchConfigurationSettings, ConfigurationOrderService configurationOrderService) {
        return new InitRefreshableConfigurationBeanPostProcessor(configServiceProvider, xmConfigProperties,
            fetchConfigurationSettings, configurationOrderService);
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :xm-commons-config:test --tests "com.icthh.xm.commons.config.client.config.InitRefreshableConfigurationBeanPostProcessorUnitTest"`
Expected: PASS — 6 existing tests (unchanged behavior with empty order cache) + 1 new ordering test

- [ ] **Step 5: Commit**

```bash
git add xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/config/InitRefreshableConfigurationBeanPostProcessor.java \
        xm-commons-config/src/main/java/com/icthh/xm/commons/config/client/config/XmConfigConfiguration.java \
        xm-commons-config/src/test/java/com/icthh/xm/commons/config/client/config/InitRefreshableConfigurationBeanPostProcessorUnitTest.java
git commit -m "feat(config): apply order.yml ordering to startup config init"
```

---

### Task 4: Full-module verification

**Files:**
- No new files. Verification only (plus any fixes it surfaces).

**Interfaces:**
- Consumes: everything from Tasks 1-3.
- Produces: a green `xm-commons-config` module and confirmation that no other module in the repo references the changed constructors.

- [ ] **Step 1: Run the whole module test suite**

Run: `./gradlew :xm-commons-config:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Verify no other in-repo call sites were missed**

Run: `grep -rn "new CommonConfigService\|new InitRefreshableConfigurationBeanPostProcessor\|extends AbstractConfigService" --include="*.java" . | grep -v "/build/"`
Expected: hits only in `xm-commons-config` main + test sources, all already updated (3-arg / 4-arg constructor calls; `CommonConfigService` is the only subclass of `AbstractConfigService`).

- [ ] **Step 3: Compile the whole repo to catch cross-module breakage**

Run: `./gradlew compileJava compileTestJava -x :xm-commons-config:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit (only if fixes were needed)**

```bash
git status --short
# if clean: nothing to do
# if fixes were made:
git add -A xm-commons-config
git commit -m "fix(config): post-verification fixes for order.yml ordering"
```

---

## Self-Review Notes

- **Spec coverage:** order.yml format + Ant patterns (Task 1), order.yml-first + unmatched-last + stability (Task 1), per-tenant isolation incl. commons (Task 1), event flow ordering + same-batch application + refreshFinished order (Task 2), startup ordering + cache seeding + refreshFinished order (Task 3), error handling — invalid YAML / deletion / sort fallback (Task 1 tests + defensive code), Spring wiring (Tasks 2-3), breaking-constructor call-site sweep (Task 4). Out-of-scope items from the spec have no tasks, as intended.
- **Type consistency:** `processOrderConfigs(Map<String, Configuration>)` and `sortPaths(Collection<String>) : List<String>` are used with exactly these signatures in Tasks 2 and 3.
- **Known intentional behavior:** existing tests keep passing because an empty order cache makes `sortPaths` an identity operation (short-circuit on `orders.isEmpty()`).
