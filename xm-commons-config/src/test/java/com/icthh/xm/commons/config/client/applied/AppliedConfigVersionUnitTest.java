package com.icthh.xm.commons.config.client.applied;

import org.junit.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AppliedConfigVersionUnitTest {

    private static final Instant AT = Instant.parse("2026-11-04T10:15:00Z");
    private static final String JSON_NESTED = "{\"mainVersion\":\"main1\",\"externalTenantVersions\":"
        + "{\"TENANTX\":{\"mainVersion\":\"x1\",\"externalTenantVersions\":{}},\"OTHER\":{\"mainVersion\":\"o1\"}}}";

    private AppliedConfigVersion parse(String raw) {
        return AppliedConfigVersion.parse(raw, "event", AT);
    }

    @Test
    public void jsonWithNestedExternalVersionsIsFlattened() {
        AppliedConfigVersion version = parse(JSON_NESTED);

        assertThat(version.mainVersion()).isEqualTo("main1");
        assertThat(version.externalTenantVersions()).isEqualTo(Map.of("TENANTX", "x1", "OTHER", "o1"));
        assertThat(version.raw()).isEqualTo(JSON_NESTED);
        assertThat(version.source()).isEqualTo("event");
        assertThat(version.updatedAt()).isEqualTo(AT);
    }

    @Test
    public void jsonWithStringExternalVersionsParses() {
        AppliedConfigVersion version = parse("{\"mainVersion\":\"m\",\"externalTenantVersions\":{\"T\":\"t1\"}}");

        assertThat(version.externalTenantVersions()).isEqualTo(Map.of("T", "t1"));
    }

    @Test
    public void plainStringIsMainVersion() {
        AppliedConfigVersion version = parse("3f2a9c");

        assertThat(version.mainVersion()).isEqualTo("3f2a9c");
        assertThat(version.raw()).isEqualTo("3f2a9c");
        assertThat(version.externalTenantVersions()).isEmpty();
    }

    @Test
    public void unknownAndMalformedJsonKeepRawOnly() {
        for (String raw : new String[] {"{\"a\":1}", "{\"mainVersion\":", "[1,2]"}) {
            AppliedConfigVersion version = parse(raw);

            assertThat(version.mainVersion()).isNull();
            assertThat(version.raw()).isEqualTo(raw);
        }
    }

    @Test
    public void blankIsUnknown() {
        assertThat(parse(null).mainVersion()).isEqualTo("unknown");
        assertThat(parse("  ").mainVersion()).isEqualTo("unknown");
    }

    @Test
    public void forTenantKeepsOnlyOwnExternalEntryAndDropsRaw() {
        AppliedConfigVersion scoped = parse(JSON_NESTED).forTenant("tenantx");

        assertThat(scoped.mainVersion()).isEqualTo("main1");
        assertThat(scoped.externalTenantVersions()).isEqualTo(Map.of("TENANTX", "x1"));
        assertThat(scoped.raw()).isNull();
    }

    @Test
    public void forTenantKeepsRawWhenItCarriesNoOtherTenant() {
        assertThat(parse("abc").forTenant("T").raw()).isEqualTo("abc");
        assertThat(parse("{\"a\":1}").forTenant("T").raw()).isNull();
    }

    @Test
    public void forTenantDropsJsonRawThatNamesAnotherTenant() {
        String json = "{\"mainVersion\":\"m\",\"externalTenantVersions\":{\"OTHER\":null}}";
        assertThat(parse(json).forTenant("T").raw()).isNull();
    }
}
