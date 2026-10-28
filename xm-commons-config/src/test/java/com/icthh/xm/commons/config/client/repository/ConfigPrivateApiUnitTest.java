package com.icthh.xm.commons.config.client.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

import static com.icthh.xm.commons.config.client.repository.ConfigPrivateApi.ED25519;
import static com.icthh.xm.commons.config.client.repository.ConfigPrivateApi.ED25519_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigPrivateApiUnitTest {

    private static final String SERVICE = "uaa";
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private final KeyPair keyPair = generateKeyPair();
    private final String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

    @Test
    public void testMigrationUsesOldApiWithoutToken() {
        ConfigPrivateApi api = new ConfigPrivateApi(true, SERVICE, null);

        assertThat(api.getUrl()).isEqualTo("/api/private");
        assertThat(api.authorize(new HttpHeaders()).containsKey(ConfigPrivateApi.TOKEN_HEADER)).isFalse();
    }

    @Test
    public void testNewApiSendsTokenSignedByPrivateKey() throws Exception {
        ConfigPrivateApi api = new ConfigPrivateApi(false, SERVICE, privateKey);

        String token = api.authorize(new HttpHeaders()).getFirst(ConfigPrivateApi.TOKEN_HEADER);

        assertThat(api.getUrl()).isEqualTo("/api/private/v2");
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        assertThat(verify(keyPair.getPublic(), parts)).isTrue();
        assertThat(decode(parts[0]).get("alg").asText()).isEqualTo("EdDSA");
        JsonNode claims = decode(parts[1]);
        assertThat(claims.get("iss").asText()).isEqualTo(SERVICE);
        assertThat(claims.get("aud").get(0).asText()).isEqualTo(ConfigPrivateApi.AUDIENCE);
        assertThat(claims.get("exp").asLong()).isEqualTo(claims.get("iat").asLong() + 300);
    }

    @Test
    public void testTokenIsReusedUntilShortlyBeforeExpiration() {
        MutableClock clock = new MutableClock(NOW);
        ConfigPrivateApi api = new ConfigPrivateApi(false, SERVICE, privateKey, clock);

        String first = api.getToken();
        clock.instant = NOW.plusSeconds(239);
        assertThat(api.getToken()).isSameAs(first);

        clock.instant = NOW.plusSeconds(241);
        assertThat(api.getToken()).isNotEqualTo(first);
    }

    @Test
    public void testMissingPrivateKeyFailsOnCreation() {
        assertThatThrownBy(() -> new ConfigPrivateApi(false, SERVICE, " "))
            .isInstanceOf(ConfigPrivateKeyNotSetException.class)
            .hasMessageContaining("application.config-private-key");
    }

    @Test
    public void testInvalidPrivateKeyFailsOnCreationWithPropertyName() {
        assertThatThrownBy(() -> new ConfigPrivateApi(false, SERVICE, "not-a-key"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("application.config-private-key");
        assertThatThrownBy(() -> new ConfigPrivateApi(false, SERVICE, "bm90LWEta2V5"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("application.config-private-key");
    }

    private static boolean verify(PublicKey publicKey, String[] parts) throws Exception {
        Signature signature = Signature.getInstance(ED25519, ED25519_PROVIDER);
        signature.initVerify(publicKey);
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return signature.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    private static JsonNode decode(String part) throws Exception {
        return new ObjectMapper().readTree(Base64.getUrlDecoder().decode(part));
    }

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance(ED25519, ED25519_PROVIDER).generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
