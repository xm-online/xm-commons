package com.icthh.xm.commons.security.jwt;

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
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
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
        this(jwtVerificationKeyClient, DefaultClock.INSTANCE);
    }

    public TokenProvider(JwtVerificationKeyClient jwtVerificationKeyClient, Clock clock) {
        Key key = jwtVerificationKeyClient.getVerificationKey();
        jwtParser = Jwts.parserBuilder().setSigningKey(key).setClock(clock).build();
    }

    public XmAuthentication getAuthentication(HttpServletRequest request, String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();

        List<String> authoritiesList = claims.get(AUTHORITIES_KEY, List.class);
        Collection<? extends GrantedAuthority> authorities = authoritiesList
            .stream()
            .filter(auth -> !auth.trim().isEmpty())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        XmAuthenticationDetails principal = new XmAuthenticationDetails(claims, request, token);
        return new XmAuthentication(principal, token, authorities);
    }

    public boolean validateToken(String authToken) {
        try {
            jwtParser.parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.info(INVALID_JWT_TOKEN, e);
        } catch (IllegalArgumentException e) {
            log.error("Token validation error {}", e.getMessage());
        }

        return false;
    }
}
