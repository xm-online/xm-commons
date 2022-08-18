package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class AuthenticationSecurityExpressionMethods {

    private final Authentication authentication;

    private Set<String> missingScopes = new LinkedHashSet<String>();

    public boolean clientHasRole(String role) {
        return clientHasAnyRole(role);
    }

    public boolean clientHasAnyRole(String... roles) {
        if (authentication instanceof XmAuthentication xmAuthentication) {
            Set<String> authorities = ofNullable(xmAuthentication.getDetails())
                    .map(XmAuthenticationDetails::getAuthorities)
                    .orElse(Set.of());
            return stream(roles).anyMatch(authorities::contains);
        }
        return false;
    }

    public boolean hasScope(String scope) {
        return hasAnyScope(scope);
    }

    public boolean hasAnyScope(String... scopes) {
        boolean result = hasAnyScope(authentication, scopes);
        if (!result) {
            missingScopes.addAll(Arrays.asList(scopes));
        }
        return result;
    }

    public boolean hasScopeMatching(String scopeRegex) {
        return hasAnyScopeMatching(scopeRegex);
    }

    public boolean hasAnyScopeMatching(String... scopesRegex) {
        boolean result = hasAnyScopeMatching(authentication, scopesRegex);
        if (!result) {
            missingScopes.addAll(Arrays.asList(scopesRegex));
        }
        return result;
    }

    private static boolean hasAnyScope(Authentication authentication, String[] scopes) {
        if (authentication instanceof XmAuthentication xmAuthentication) {
            Set<String> scope = ofNullable(xmAuthentication.getDetails())
                    .map(XmAuthenticationDetails::getScope)
                    .orElse(Set.of());
            return stream(scopes).anyMatch(scope::contains);
        }
        return false;
    }

    private static boolean hasAnyScopeMatching(Authentication authentication, String[] scopesRegex) {

        if (authentication instanceof XmAuthentication xmAuthentication) {
            Set<String> scopes = ofNullable(xmAuthentication.getDetails())
                    .map(XmAuthenticationDetails::getScope)
                    .orElse(Set.of());

            for (String scope : scopes) {
                for (String regex : scopesRegex) {
                    if (scope.matches(regex)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
