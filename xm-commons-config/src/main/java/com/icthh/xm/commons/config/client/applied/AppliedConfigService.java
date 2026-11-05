package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.client.api.ConfigurationChangedListener;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Applied configuration state of this service: the ms-config version and a hash per applied config path.
 * Read methods apply no access rules, except {@link #getAppliedConfig}, which scopes the data to the caller.
 *
 * <p>Hashes only are stored, updated per changed path. Aggregates are computed per call from the stored hashes,
 * never from contents.
 */
@Slf4j
public class AppliedConfigService implements ConfigurationChangedListener {

    public static final String COMMONS = "commons";

    private static final String TENANTS_PREFIX = "/config/tenants/";
    private static final Comparator<String> UTF8_ORDER = Comparator.comparing(s -> s.getBytes(UTF_8),
        Arrays::compareUnsigned);

    private final CommonConfigRepository repository;
    private final FetchConfigurationSettings fetchSettings;
    private final boolean versioned;
    private final String managementRole;
    private final XmAuthenticationContextHolder authContextHolder = new SpringSecurityXmAuthenticationContextHolder();

    private final ConcurrentSkipListMap<String, String> hashes = new ConcurrentSkipListMap<>(UTF8_ORDER);
    private volatile AppliedConfigVersion version =
        AppliedConfigVersion.unknown(AppliedConfigVersion.SOURCE_STARTUP, Instant.now());

    /**
     * @param versioned false when the repository has no versions (FILE mode): event versions are ignored
     * @param managementRole authority that sees every tenant, {@code application.management.access.role}
     */
    public AppliedConfigService(CommonConfigRepository repository, FetchConfigurationSettings fetchSettings,
                                boolean versioned, String managementRole) {
        this.repository = repository;
        this.fetchSettings = fetchSettings;
        this.versioned = versioned;
        this.managementRole = managementRole;
    }

    public record PathHash(String path, String hashSum) {
    }

    public record Aggregate(String aggregate, int configCount) {
    }

    public record FetchScope(boolean fetchAll, List<String> msConfigPatterns) {
    }

    public AppliedConfigVersion getVersion() {
        return version;
    }

    public FetchScope getFetchScope() {
        return new FetchScope(Boolean.TRUE.equals(fetchSettings.getIsFetchAll()), fetchSettings.getMsConfigPatterns());
    }

    /** All applied paths with {@code sha256Hex(content)}, sorted by path in UTF-8 byte order. */
    public List<PathHash> getPathHashes() {
        return hashes.entrySet().stream().map(e -> new PathHash(e.getKey(), e.getValue())).toList();
    }

    public List<PathHash> getPathHashes(String tenantKey) {
        return filterByKey(getPathHashes(), tenantKey);
    }

    public Map<String, Aggregate> getAggregates() {
        return aggregatesByTenantKey(getPathHashes());
    }

    public Aggregate getAggregate(String tenantKey) {
        return aggregateOf(getPathHashes(tenantKey));
    }

    public Aggregate getTotal() {
        return aggregateOf(getPathHashes());
    }

    /**
     * Tenant key of a path, as in xm-ms-config: {@code /config/tenants/{TENANT}/...} gives TENANT, except
     * {@code /config/tenants/commons/**}; every path outside a tenant gives {@code commons}.
     */
    public static String tenantKeyOf(String path) {
        if (!path.startsWith(TENANTS_PREFIX)) {
            return COMMONS;
        }
        int end = path.indexOf('/', TENANTS_PREFIX.length());
        if (end < 0 || end == path.length() - 1) {
            return COMMONS;
        }
        String tenant = path.substring(TENANTS_PREFIX.length(), end);
        return tenant.isEmpty() ? COMMONS : tenant;
    }

    /**
     * Aggregate of a {@code {path, hashSum}} list: SHA-256 hex of the UTF-8 text made of the lines
     * {@code path + ":" + hashSum + "\n"}, sorted by path in UTF-8 byte order (not by whole line), with nothing
     * added after the last line. An empty list gives the SHA-256 of the empty text.
     */
    public static Aggregate aggregateOf(List<PathHash> pathHashes) {
        StringBuilder text = new StringBuilder();
        pathHashes.stream()
            .sorted(Comparator.comparing(PathHash::path, UTF8_ORDER))
            .forEach(it -> text.append(it.path()).append(':').append(it.hashSum()).append('\n'));
        return new Aggregate(DigestUtils.sha256Hex(text.toString().getBytes(UTF_8)), pathHashes.size());
    }

    /** One aggregate per tenant key present in the list. */
    private static Map<String, Aggregate> aggregatesByTenantKey(List<PathHash> pathHashes) {
        return pathHashes.stream().collect(Collectors.groupingBy(it -> tenantKeyOf(it.path()), TreeMap::new,
            Collectors.collectingAndThen(Collectors.toList(), AppliedConfigService::aggregateOf)));
    }

    /** Hashes of one tenant key, matched ignoring case. */
    private static List<PathHash> filterByKey(List<PathHash> pathHashes, String tenantKey) {
        return pathHashes.stream().filter(it -> tenantKeyOf(it.path()).equalsIgnoreCase(tenantKey)).toList();
    }

    /**
     * The applied config for the current caller. The management role gets every tenant key, the total and the
     * full version; any other caller gets its own tenant plus {@code commons}, and asking for another tenant is
     * refused with {@link AccessDeniedException}.
     */
    public AppliedConfigResponse getAppliedConfig(String tenant, boolean details) {
        XmAuthenticationContext context = authContextHolder.getContext();
        boolean management = context.getAuthorities().stream()
            .anyMatch(it -> managementRole.equals(it.getAuthority()));
        String ownTenant = context.getTenantName().map(this::upper).orElse(null);
        String requested = tenant == null ? ownTenant : upper(tenant);
        if (!management && (ownTenant == null || !ownTenant.equals(requested))) {
            throw new AccessDeniedException("Applied config of another tenant is not available");
        }
        List<PathHash> all = getPathHashes();
        boolean allKeys = management && tenant == null;
        List<PathHash> inScope = allKeys ? all : all.stream().filter(it -> inTenantScope(it, requested)).toList();
        Map<String, Aggregate> hashes = allKeys ? aggregatesByTenantKey(all) : tenantAggregates(all, requested);
        AppliedConfigVersion commit = management ? version : version.forTenant(requested);
        Aggregate total = management ? aggregateOf(all) : null;
        return new AppliedConfigResponse(commit, getFetchScope(), hashes, total, details ? inScope : null);
    }

    private Map<String, Aggregate> tenantAggregates(List<PathHash> all, String tenant) {
        Map<String, Aggregate> result = new TreeMap<>();
        result.put(tenant, aggregateOf(filterByKey(all, tenant)));
        result.put(COMMONS, aggregateOf(filterByKey(all, COMMONS)));
        return result;
    }

    private boolean inTenantScope(PathHash pathHash, String tenant) {
        String key = tenantKeyOf(pathHash.path());
        return key.equalsIgnoreCase(tenant) || COMMONS.equals(key);
    }

    private String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    /** Asks ms-config for its current version before the initial config load. Never throws. */
    public void loadStartupVersion() {
        try {
            repository.getVersion().ifPresent(
                raw -> record(AppliedConfigVersion.parse(raw, AppliedConfigVersion.SOURCE_STARTUP, Instant.now())));
        } catch (RuntimeException e) {
            log.warn("Config version from ms-config is unavailable at startup, commit is unknown: {}", e.toString());
        }
    }

    public void onInitialLoad(Map<String, Configuration> configMap) {
        long start = System.currentTimeMillis();
        configMap.forEach((path, configuration) -> putHash(path, configuration.getContent()));
        log.info("Applied config hashes initialized: {} configs in {} tenant keys, took {} ms",
            hashes.size(), getAggregates().size(), System.currentTimeMillis() - start);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        putHash(configuration.getPath(), configuration.getContent());
    }

    /** Records the version of a config event, to be called after the event's configurations were applied. */
    public void recordEventVersion(String commit) {
        if (versioned && commit != null && !commit.isBlank()) {
            record(AppliedConfigVersion.parse(commit, AppliedConfigVersion.SOURCE_EVENT, Instant.now()));
        }
    }

    private void putHash(String path, String content) {
        if (content == null) {
            hashes.remove(path);
            log.debug("Applied config hash removed: {}", path);
        } else {
            String hash = DigestUtils.sha256Hex(content);
            hashes.put(path, hash);
            log.debug("Applied config hash updated: {} {}", path, hash);
        }
    }

    private synchronized void record(AppliedConfigVersion newVersion) {
        AppliedConfigVersion old = version;
        if (old.sameVersion(newVersion)) {
            return;
        }
        version = newVersion;
        log.info("Applied config version changed: {} {} -> {} {}, source: {}", old.mainVersion(),
            old.externalTenantVersions(), newVersion.mainVersion(), newVersion.externalTenantVersions(),
            newVersion.source());
    }
}
