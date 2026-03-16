package com.icthh.xm.commons.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.security.oauth2.JwtVerificationKeyClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.impl.DefaultClock;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "authorities";

    private static final String INVALID_JWT_TOKEN = "Invalid JWT token.";

    private final JwtParser jwtParser;

    private final Clock jwtClock;

    private final Cache<String, Claims> tokenCache;

    @Autowired
    public TokenProvider(JwtVerificationKeyClient jwtVerificationKeyClient,
                         @Value("${application.jwt.token-cache-max-size:10000}") Integer tokenCacheMaxSize,
                         @Value("${application.jwt.token-cache-expiration-seconds:300}") Integer tokenCacheExpirationInSeconds) {
        this(jwtVerificationKeyClient, DefaultClock.INSTANCE, tokenCacheMaxSize, tokenCacheExpirationInSeconds);
    }

    public TokenProvider(JwtVerificationKeyClient jwtVerificationKeyClient,
                         Clock clock,
                         Integer tokenCacheMaxSize,
                         Integer tokenCacheExpirationInSeconds) {
        PublicKey key = jwtVerificationKeyClient.getVerificationKey();
        jwtClock = clock;
        jwtParser = Jwts.parser().verifyWith(key).clock(clock).build();
        tokenCache = Caffeine.newBuilder()
            .maximumSize(tokenCacheMaxSize)
            .expireAfterWrite(Duration.ofSeconds(tokenCacheExpirationInSeconds))
            .build();
    }

    public XmAuthentication getAuthentication(HttpServletRequest request, String token, Claims claims) {
        Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);

        XmAuthenticationDetails principal = new XmAuthenticationDetails(claims, request, token);
        return new XmAuthentication(principal, token, authorities);
    }

    public XmAuthentication getAuthentication(ServerHttpRequest request, String token, Claims claims) {
        Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);

        XmAuthenticationDetails principal = new XmAuthenticationDetails(claims, request, token);
        return new XmAuthentication(principal, token, authorities);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Claims claims) {
        List<String> authoritiesList = claims.get(AUTHORITIES_KEY, List.class);

        return authoritiesList.stream()
            .filter(auth -> !auth.trim().isEmpty())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    public Claims validateToken(String authToken) {
        try {
            return getClaims(authToken);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.info(INVALID_JWT_TOKEN, e);
        } catch (IllegalArgumentException e) {
            log.error("Token validation error {}", e.getMessage());
        }

        return null;
    }

    public Claims getClaims(String token) {
        Claims claims = tokenCache.get(token, t -> jwtParser.parseSignedClaims(t).getPayload());
        if (claims.getExpiration().before(jwtClock.now())) {
            tokenCache.invalidate(token);
            throw new ExpiredJwtException(null, claims, "Token was expired");
        }
        return claims;
    }
}
