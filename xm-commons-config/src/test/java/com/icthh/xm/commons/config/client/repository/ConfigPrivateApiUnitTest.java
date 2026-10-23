package com.icthh.xm.commons.config.client.repository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigPrivateApiUnitTest {

    private static final String SERVICE = "entity";
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private final KeyPair keyPair = generateKeyPair();
    private final String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

    @Test
    public void testMigrationUsesOldApiWithoutToken() {
        ConfigPrivateApi api = new ConfigPrivateApi(true, SERVICE, null);

        assertThat(api.getUrl()).isEqualTo("/api/private");
        assertThat(api.authorize(new HttpHeaders()).containsHeader(ConfigPrivateApi.TOKEN_HEADER)).isFalse();
    }

    @Test
    public void testNewApiSendsTokenSignedByPrivateKey() {
        ConfigPrivateApi api = new ConfigPrivateApi(false, SERVICE, privateKey);

        String token = api.authorize(new HttpHeaders()).getFirst(ConfigPrivateApi.TOKEN_HEADER);

        assertThat(api.getUrl()).isEqualTo("/api/private/v2");
        Claims claims = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(token).getPayload();
        assertThat(claims.getIssuer()).isEqualTo(SERVICE);
        assertThat(claims.getAudience()).containsExactly(ConfigPrivateApi.AUDIENCE);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(claims.getIssuedAt().toInstant().plusSeconds(300));
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

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
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
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
