package com.icthh.xm.commons.config.client.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigPrivateApi {

    public static final String TOKEN_HEADER = "X-Config-Token";
    public static final String AUDIENCE = "config";

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_REFRESH_BEFORE = Duration.ofMinutes(1);
    static final Provider ED25519_PROVIDER = new BouncyCastleProvider();
    static final String ED25519 = "Ed25519";

    private static final String URL = "/api/private";
    private static final String URL_WITH_TOKEN = "/api/private/v2";
    private static final String JWT_HEADER = "{\"alg\":\"EdDSA\"}";

    private final ObjectMapper mapper = new ObjectMapper();
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
            token = sign(now, tokenExpiration);
        }
        return token;
    }

    @SneakyThrows
    private String sign(Instant issuedAt, Instant expiration) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", serviceName);
        claims.put("aud", List.of(AUDIENCE));
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiration.getEpochSecond());
        String content = encode(JWT_HEADER.getBytes(StandardCharsets.UTF_8)) + "."
            + encode(mapper.writeValueAsBytes(claims));

        Signature signature = Signature.getInstance(ED25519, ED25519_PROVIDER);
        signature.initSign(privateKey);
        signature.update(content.getBytes(StandardCharsets.US_ASCII));
        return content + "." + encode(signature.sign());
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @SneakyThrows
    private static PrivateKey readPrivateKey(String privateKey) {
        if (StringUtils.isBlank(privateKey)) {
            throw new ConfigPrivateKeyNotSetException();
        }
        try {
            byte[] encoded = Base64.getDecoder().decode(privateKey.trim());
            return KeyFactory.getInstance(ED25519, ED25519_PROVIDER).generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (IllegalArgumentException | InvalidKeySpecException e) {
            throw new IllegalStateException("application.config-private-key is not a base64 PKCS#8 DER "
                + "Ed25519 private key: " + e.getMessage(), e);
        }
    }
}
