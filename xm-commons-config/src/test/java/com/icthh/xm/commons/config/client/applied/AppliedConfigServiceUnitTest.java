package com.icthh.xm.commons.config.client.applied;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.Aggregate;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.PathHash;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppliedConfigServiceUnitTest {

    private final CommonConfigRepository repository = mock(CommonConfigRepository.class);
    private final FetchConfigurationSettings settings = new FetchConfigurationSettings("entity", false);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(AppliedConfigService.class);

    private AppliedConfigService service = newService(true);

    private AppliedConfigService newService(boolean versioned) {
        return new AppliedConfigService(repository, settings, versioned, "SYSTEM-ADMIN");
    }

    @Before
    public void attachAppender() {
        logs.start();
        serviceLogger.addAppender(logs);
    }

    @After
    public void detachAppender() {
        serviceLogger.detachAppender(logs);
    }

    private static Configuration config(String path, String content) {
        return new Configuration(path, content);
    }

    @Test
    public void incrementalHashesEqualFullRecompute() {
        service.onInitialLoad(Map.of(
            "/config/tenants/XM/entity/a.yml", config("/config/tenants/XM/entity/a.yml", "a"),
            "/config/tenants/XM/entity/b.yml", config("/config/tenants/XM/entity/b.yml", "b")));
        service.onConfigurationChanged(config("/config/tenants/XM/entity/c.yml", "c"));
        service.onConfigurationChanged(config("/config/tenants/XM/entity/a.yml", "a2"));
        service.onConfigurationChanged(config("/config/tenants/XM/entity/b.yml", null));
        service.onConfigurationChanged(config("/config/tenants/commons/x.yml", "x"));
        service.onConfigurationChanged(config("/config/tenants/commons/x.yml", "x3"));

        AppliedConfigService fresh = newService(true);
        fresh.onInitialLoad(Map.of(
            "/config/tenants/XM/entity/a.yml", config("/config/tenants/XM/entity/a.yml", "a2"),
            "/config/tenants/XM/entity/c.yml", config("/config/tenants/XM/entity/c.yml", "c"),
            "/config/tenants/commons/x.yml", config("/config/tenants/commons/x.yml", "x3")));

        assertThat(service.getPathHashes()).isEqualTo(fresh.getPathHashes()).hasSize(3);
        assertThat(service.getAggregates()).isEqualTo(fresh.getAggregates());
        assertThat(service.getTotal()).isEqualTo(fresh.getTotal());
    }

    @Test
    public void pathHashIsSha256HexOfContent() {
        service.onConfigurationChanged(config("/p", "abc"));
        service.onConfigurationChanged(config("/q", "Привіт, світе ✓"));

        assertThat(service.getPathHashes()).containsExactly(
            new PathHash("/p", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
            new PathHash("/q", sha256("Привіт, світе ✓")));
    }

    @Test
    public void aggregateEqualsDocumentedFormula() {
        service.onConfigurationChanged(config("/config/tenants/XM/a/b", "1"));
        service.onConfigurationChanged(config("/config/tenants/XM/a/b.yml", "2"));
        service.onConfigurationChanged(config("/config/tenants/XM/a-b/c", "3"));
        service.onConfigurationChanged(config("/config/tenants/commons/z", "4"));

        List<PathHash> xm = service.getPathHashes("XM");
        assertThat(service.getAggregate("XM")).isEqualTo(new Aggregate(formula(xm), 3));
        assertThat(service.getAggregate("commons")).isEqualTo(new Aggregate(formula(service.getPathHashes("commons")), 1));
        assertThat(service.getTotal()).isEqualTo(new Aggregate(formula(service.getPathHashes()), 4));
        assertThat(service.getPathHashes()).extracting(PathHash::path)
            .containsExactly("/config/tenants/XM/a-b/c", "/config/tenants/XM/a/b", "/config/tenants/XM/a/b.yml",
                "/config/tenants/commons/z");
    }

    @Test
    public void emptyAggregateIsHashOfEmptyText() {
        assertThat(service.getAggregate("XM")).isEqualTo(new Aggregate(sha256(""), 0));
    }

    @Test
    public void tenantKeyGrouping() {
        assertThat(AppliedConfigService.tenantKeyOf("/config/tenants/commons/x")).isEqualTo("commons");
        assertThat(AppliedConfigService.tenantKeyOf("/other/x")).isEqualTo("commons");
        assertThat(AppliedConfigService.tenantKeyOf("/config/tenants/X/commons/y")).isEqualTo("X");
        assertThat(AppliedConfigService.tenantKeyOf("/config/tenants/X/entity/y.yml")).isEqualTo("X");
        assertThat(AppliedConfigService.tenantKeyOf("/config/tenants/tenants-list.json")).isEqualTo("commons");
    }

    @Test
    public void singlePathRefreshOf10kConfigsChangesOnlyItsKey() {
        Map<String, Configuration> all = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            String path = "/config/tenants/T" + (i % 10) + "/entity/f" + i + ".yml";
            all.put(path, config(path, "content " + i));
        }
        service.onInitialLoad(all);
        Map<String, Aggregate> before = service.getAggregates();
        String changed = "/config/tenants/T3/entity/f3.yml";

        long start = System.nanoTime();
        service.onConfigurationChanged(config(changed, "x".repeat(5_000_000)));
        Map<String, Aggregate> after = service.getAggregates();
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertThat(service.getPathHashes()).hasSize(10_000);
        assertThat(after.get("T3")).isNotEqualTo(before.get("T3"));
        before.keySet().stream().filter(k -> !k.equals("T3"))
            .forEach(k -> assertThat(after.get(k)).isEqualTo(before.get(k)));
        assertThat(after.get("T3").aggregate()).isEqualTo(formula(service.getPathHashes("T3")));
        assertThat(millis).as("update plus aggregates for 10k configs, smoke bound").isLessThan(3000);
    }

    @Test
    public void startupVersionFromRepositoryIsRecordedWithSource() {
        when(repository.getVersion()).thenReturn(Optional.of("{\"mainVersion\":\"m1\"}"));

        service.loadStartupVersion();

        assertThat(service.getVersion().mainVersion()).isEqualTo("m1");
        assertThat(service.getVersion().source()).isEqualTo("startup");
        assertThat(messages(Level.INFO)).anyMatch(m -> m.startsWith("Applied config version changed"));
    }

    @Test
    public void startupFailureKeepsUnknownWithOneWarn() {
        when(repository.getVersion()).thenThrow(new IllegalStateException("timeout"));

        service.loadStartupVersion();

        assertThat(service.getVersion().mainVersion()).isEqualTo("unknown");
        assertThat(messages(Level.WARN)).hasSize(1).allMatch(m -> m.contains("timeout"));
    }

    @Test
    public void emptyStartupVersionKeepsUnknown() {
        when(repository.getVersion()).thenReturn(Optional.empty());

        service.loadStartupVersion();

        assertThat(service.getVersion().mainVersion()).isEqualTo("unknown");
    }

    @Test
    public void eventVersionIsRecordedAndSameVersionKeepsSourceAndTime() {
        service.recordEventVersion("abc");
        AppliedConfigVersion first = service.getVersion();
        service.recordEventVersion("abc");

        assertThat(first.mainVersion()).isEqualTo("abc");
        assertThat(first.source()).isEqualTo("event");
        assertThat(service.getVersion()).isSameAs(first);
        assertThat(messages(Level.INFO)).hasSize(1);
    }

    @Test
    public void blankEventCommitKeepsCurrentVersion() {
        service.recordEventVersion("abc");
        service.recordEventVersion("");
        service.recordEventVersion(null);

        assertThat(service.getVersion().mainVersion()).isEqualTo("abc");
    }

    @Test
    public void notVersionedIgnoresEventVersions() {
        service = newService(false);

        service.recordEventVersion("abc");

        assertThat(service.getVersion().mainVersion()).isEqualTo("unknown");
    }

    @Test
    public void fetchScopeReportsPatternsAndFetchAll() {
        assertThat(service.getFetchScope().fetchAll()).isFalse();
        assertThat(service.getFetchScope().msConfigPatterns()).contains("/config/tenants/{tenantName}/entity/**");
    }

    private List<String> messages(Level level) {
        List<String> result = new ArrayList<>();
        logs.list.stream().filter(e -> e.getLevel() == level).forEach(e -> result.add(e.getFormattedMessage()));
        return result;
    }

    private static String sha256(String text) {
        return DigestUtils.sha256Hex(text);
    }

    /** Independent implementation of the documented formula: sort by UTF-8 bytes of path, join lines, sha256. */
    private static String formula(List<PathHash> list) {
        List<PathHash> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> java.util.Arrays.compareUnsigned(a.path().getBytes(StandardCharsets.UTF_8),
            b.path().getBytes(StandardCharsets.UTF_8)));
        MessageDigest digest = DigestUtils.getSha256Digest();
        sorted.forEach(it -> digest.update((it.path() + ":" + it.hashSum() + "\n").getBytes(StandardCharsets.UTF_8)));
        return org.apache.commons.codec.binary.Hex.encodeHexString(digest.digest());
    }
}
