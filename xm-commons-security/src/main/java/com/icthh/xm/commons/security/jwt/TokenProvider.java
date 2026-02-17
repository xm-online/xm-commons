package com.icthh.xm.commons.security.jwt;

import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.security.oauth2.JwtVerificationKeyClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "authorities";

    private static final String INVALID_JWT_TOKEN = "Invalid JWT token.";

    private final JwtParser jwtParser;

    @Autowired
    public TokenProvider(JwtVerificationKeyClient jwtVerificationKeyClient) {
        this(jwtVerificationKeyClient, Clock.systemUTC());
    }

    public TokenProvider(JwtVerificationKeyClient jwtVerificationKeyClient, Clock clock) {
        Key key = jwtVerificationKeyClient.getVerificationKey();
        var parserBuilder = Jwts.parser().clock(() -> Date.from(clock.instant()));

        if (key instanceof SecretKey secretKey) {
            parserBuilder.verifyWith(secretKey);
        } else if (key instanceof PublicKey publicKey) {
            parserBuilder.verifyWith(publicKey);
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());
        }

        jwtParser = parserBuilder.build();
    }

    public XmAuthentication getAuthentication(HttpServletRequest request, String token) {
        final String normalizedToken = token.trim();
        Claims claims = jwtParser.parseSignedClaims(normalizedToken).getPayload();
        Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);

        XmAuthenticationDetails principal = new XmAuthenticationDetails(claims, request, normalizedToken);
        return new XmAuthentication(principal, normalizedToken, authorities);
    }

    public XmAuthentication getAuthentication(ServerHttpRequest request, String token) {
        final String normalizedToken = token.trim();
        Claims claims = this.jwtParser.parseSignedClaims(normalizedToken).getPayload();
        Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);

        XmAuthenticationDetails principal = new XmAuthenticationDetails(claims, request, normalizedToken);
        return new XmAuthentication(principal, normalizedToken, authorities);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Claims claims) {
        List<String> authoritiesList = claims.get(AUTHORITIES_KEY, List.class);

        return authoritiesList.stream()
            .filter(auth -> !auth.trim().isEmpty())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    public boolean validateToken(String authToken) {
        try {
            jwtParser.parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.info(INVALID_JWT_TOKEN, e);
        } catch (IllegalArgumentException e) {
            log.error("Token validation error {}", e.getMessage());
        }

        return false;
    }
}
