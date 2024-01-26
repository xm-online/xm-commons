package com.icthh.xm.commons.security.internal;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

@Getter
public class XmAuthenticationDetails {

    private final Long createTokenTime;
    private final String userName;
    private final Set<String> scope;
    private final String roleKey;
    private final String userKey;
    private final List<XmLogin> logins;
    private final Set<String> authorities;
    private final String clientId;

    private final String remoteAddress;
    private final String sessionId;
    private final String tokenValue;
    private final String tokenType;
    private final Map<String, Object> decodedDetails;

    public XmAuthenticationDetails(Claims claims, HttpServletRequest request, String token) {
        this.createTokenTime =  claims.get("createTokenTime", Long.class);
        this.userName =  claims.get("user_name", String.class);
        this.roleKey =  claims.get("role_key", String.class);
        this.userKey =  claims.get("user_key", String.class);
        this.clientId =  claims.get("client_id", String.class);
        this.authorities = toSet(claims.get("authorities", List.class));
        this.scope = toSet(claims.get("scope", List.class));

        List<Map<String, String>> logins = nullSafe(claims.get("logins", List.class));
        this.logins = logins.stream().map(it -> new XmLogin(it.get("typeKey"), it.get("stateKey"), it.get("login"))).toList();

        this.tokenValue = token;
        this.tokenType = "Bearer";

        this.remoteAddress = request.getRemoteAddr();
        HttpSession session = request.getSession(false);
        this.sessionId = (session != null) ? session.getId() : null;

        this.decodedDetails = unmodifiableMap(claims);
    }

    private Set<String> toSet(List<String> list) {
        return unmodifiableSet(new HashSet<>(nullSafe(list)));
    }

    private <T> List<T> nullSafe(List<T> scope) {
        return scope == null ? emptyList() : scope;
    }

    @Getter
    @RequiredArgsConstructor
    public static class XmLogin {
        private final String typeKey;
        private final String stateKey;
        private final String login;
    }

}
