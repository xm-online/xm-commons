package com.icthh.xm.commons.config.client.applied;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.icthh.xm.commons.tenant.JsonMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The xm-ms-config version the applied configurations correspond to.
 *
 * <p>ms-config sends the version as JSON of its {@code ConfigVersion}
 * ({@code mainVersion} plus {@code externalTenantVersions}) or as a plain string.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppliedConfigVersion(String mainVersion,
                                   Map<String, String> externalTenantVersions,
                                   String raw,
                                   String source,
                                   Instant updatedAt) {

    public static final String UNKNOWN = "unknown";
    public static final String SOURCE_STARTUP = "startup";
    public static final String SOURCE_EVENT = "event";

    private static final Logger LOG = LoggerFactory.getLogger(AppliedConfigVersion.class);
    private static final ObjectMapper MAPPER = JsonMapperUtils.getJsonMapperWithIgnore();

    public AppliedConfigVersion {
        externalTenantVersions = externalTenantVersions == null ? Map.of() : Map.copyOf(externalTenantVersions);
    }

    public static AppliedConfigVersion unknown(String source, Instant at) {
        return new AppliedConfigVersion(UNKNOWN, Map.of(), null, source, at);
    }

    /**
     * Parses an ms-config version. Never throws: a plain string becomes {@code mainVersion},
     * unknown or malformed JSON is kept in {@code raw} only.
     */
    public static AppliedConfigVersion parse(String raw, String source, Instant at) {
        if (StringUtils.isBlank(raw)) {
            return unknown(source, at);
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseJson(raw, trimmed, source, at);
        }
        return new AppliedConfigVersion(trimmed, Map.of(), raw, source, at);
    }

    private static AppliedConfigVersion parseJson(String raw, String trimmed, String source, Instant at) {
        try {
            JsonNode root = MAPPER.readTree(trimmed);
            JsonNode main = root.get("mainVersion");
            if (main != null && main.isString()) {
                return new AppliedConfigVersion(main.asString(), externalVersions(root), raw, source, at);
            }
        } catch (RuntimeException e) {
            LOG.debug("Config version is not a ConfigVersion JSON: {}", e.toString());
        }
        return new AppliedConfigVersion(null, Map.of(), raw, source, at);
    }

    private static Map<String, String> externalVersions(JsonNode root) {
        Map<String, String> result = new TreeMap<>();
        JsonNode external = root.get("externalTenantVersions");
        if (external != null && external.isObject()) {
            external.properties().forEach(e -> {
                String version = versionOf(e.getValue());
                if (version != null) {
                    result.put(e.getKey(), version);
                }
            });
        }
        return result;
    }

    private static String versionOf(JsonNode node) {
        JsonNode value = node.isObject() ? node.get("mainVersion") : node;
        return value != null && value.isString() ? value.asString() : null;
    }

    /** Compares the version itself, ignoring source and time. */
    public boolean sameVersion(AppliedConfigVersion other) {
        return Objects.equals(mainVersion, other.mainVersion)
            && externalTenantVersions.equals(other.externalTenantVersions)
            && (mainVersion != null || Objects.equals(raw, other.raw));
    }

    /**
     * The view for a caller limited to one tenant: only its own external entry, and {@code raw} only when it
     * is a plain string (JSON raw may name other tenants, also in entries that were not parsed).
     */
    public AppliedConfigVersion forTenant(String tenant) {
        Map<String, String> own = new TreeMap<>();
        externalTenantVersions.forEach((key, version) -> {
            if (key.equalsIgnoreCase(tenant)) {
                own.put(key, version);
            }
        });
        boolean rawIsSafe = raw != null && !raw.trim().startsWith("{") && !raw.trim().startsWith("[");
        return new AppliedConfigVersion(mainVersion, own, rawIsSafe ? raw : null, source, updatedAt);
    }
}
