package com.icthh.xm.commons.config.client.repository;

import io.jsonwebtoken.Jwts;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * The private API of xm-ms-config: with the profile migration the old /api/private without authorization,
 * otherwise /api/private/v2 with a token signed by application.config-private-key.
 * <p>
 * The token is a short JWT signed by the service itself (Ed25519): any node of xm-ms-config verifies it with the
 * public key, the nodes do not share a state.
 */
public class ConfigPrivateApi {

    public static final String TOKEN_HEADER = "X-Config-Token";
    public static final String AUDIENCE = "config";

    static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    static final Duration TOKEN_REFRESH_BEFORE = Duration.ofMinutes(1);

    private static final String URL = "/api/private";
    private static final String URL_WITH_TOKEN = "/api/private/v2";

    private final boolean migration;
    private final String serviceName;
    private final PrivateKey privateKey;
    private final Clock clock;

    private String token;
    private Instant tokenExpiration;

    public ConfigPrivateApi(boolean migration, String serviceName, String privateKey) {
        this(migration, serviceName, privateKey, Clock.systemUTC());
    }

    ConfigPrivateApi(boolean migration, String serviceName, String privateKey, Clock clock) {
        this.migration = migration;
        this.serviceName = serviceName;
        this.privateKey = migration ? null : readPrivateKey(privateKey);
        this.clock = clock;
    }

    public String getUrl() {
        return migration ? URL : URL_WITH_TOKEN;
    }

    public HttpHeaders authorize(HttpHeaders headers) {
        if (!migration) {
            headers.set(TOKEN_HEADER, getToken());
        }
        return headers;
    }

    synchronized String getToken() {
        Instant now = clock.instant();
        if (token == null || now.isAfter(tokenExpiration.minus(TOKEN_REFRESH_BEFORE))) {
            tokenExpiration = now.plus(TOKEN_TTL);
            token = Jwts.builder()
                .issuer(serviceName)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(tokenExpiration))
                .signWith(privateKey, Jwts.SIG.EdDSA)
                .compact();
        }
        return token;
    }

    @SneakyThrows
    private static PrivateKey readPrivateKey(String privateKey) {
        if (StringUtils.isBlank(privateKey)) {
            throw new ConfigPrivateKeyNotSetException();
        }
        byte[] encoded = Base64.getDecoder().decode(privateKey.trim());
        return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
}
