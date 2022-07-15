package com.icthh.xm.commons.security.internal;

import com.icthh.xm.commons.security.XmAuthenticationConstants;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

/**
 * The {@link SpringSecurityXmAuthenticationContext} class.
 */
class SpringSecurityXmAuthenticationContext implements XmAuthenticationContext {

    private static final Class<?> ANONYMOUS_AUTH_CLASS = AnonymousAuthenticationToken.class;
    private static final Class<?> REMEMBER_ME_AUTH_CLASS = RememberMeAuthenticationToken.class;

    private final SecurityContext securityContext;

    SpringSecurityXmAuthenticationContext(SecurityContext securityContext) {
        this.securityContext = Objects.requireNonNull(securityContext, "securityContext can't be null");
    }

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(securityContext.getAuthentication());
    }

    // DO not use this method in LEP! It will be removed in future!
    private Optional<OAuth2Authentication> getOAuth2Authentication() {
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(it -> it instanceof OAuth2Authentication)
                .map(it -> (OAuth2Authentication) it);
    }

    private Optional<Object> getDetails() {
        return getAuthentication().map(Authentication::getDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAuthentication() {
        return getAuthentication().isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnonymous() {
        return getAuthentication().filter(auth -> ANONYMOUS_AUTH_CLASS.isAssignableFrom(auth.getClass())).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthenticated() {
        return !isAnonymous();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRememberMe() {
        return getAuthentication().filter(auth -> REMEMBER_ME_AUTH_CLASS.isAssignableFrom(auth.getClass())).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFullyAuthenticated() {
        return getAuthentication().filter(auth -> !ANONYMOUS_AUTH_CLASS.isAssignableFrom(auth.getClass()) &&
            !REMEMBER_ME_AUTH_CLASS.isAssignableFrom(auth.getClass())).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getLogin() {
        return getAuthentication().map(auth -> {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails springSecurityUser = UserDetails.class.cast(principal);
                return springSecurityUser.getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }

            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequiredLogin() {
        return getLogin().orElseThrow(() -> new IllegalStateException("Can't get login without authentication object"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getRemoteAddress() {
        return getDetails().flatMap(details -> {
            String address;
            if (details instanceof OAuth2AuthenticationDetails) {
                address = OAuth2AuthenticationDetails.class.cast(details).getRemoteAddress();
            } else if (details instanceof WebAuthenticationDetails) {
                address = WebAuthenticationDetails.class.cast(details).getRemoteAddress();
            } else {
                throw new IllegalStateException("Unsupported auth details type " + details.getClass());
            }

            return Optional.ofNullable(address);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getSessionId() {
        return getDetails().flatMap(details -> {
            String sessionId;
            if (details instanceof OAuth2AuthenticationDetails) {
                sessionId = OAuth2AuthenticationDetails.class.cast(details).getSessionId();
            } else if (details instanceof WebAuthenticationDetails) {
                sessionId = WebAuthenticationDetails.class.cast(details).getSessionId();
            } else {
                throw new IllegalStateException("Unsupported auth details type " + details.getClass());
            }

            return Optional.ofNullable(sessionId);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getUserKey() {
        return getDetailsValue(XmAuthenticationConstants.AUTH_DETAILS_USER_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequiredUserKey() {
        return getUserKey().orElseThrow(() ->
            new IllegalStateException("Can't get userKey without authentication object"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getTokenValue() {
        return getDetails().flatMap(details -> {
            String tokenValue;
            if (details instanceof OAuth2AuthenticationDetails) {
                tokenValue = OAuth2AuthenticationDetails.class.cast(details).getTokenValue();
            } else {
                throw new IllegalStateException("Unsupported token auth details type " + details.getClass());
            }

            return Optional.ofNullable(tokenValue);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getTokenType() {
        return getDetails().flatMap(details -> {
            String tokenType;
            if (details instanceof OAuth2AuthenticationDetails) {
                tokenType = OAuth2AuthenticationDetails.class.cast(details).getTokenType();
            } else {
                throw new IllegalStateException("Unsupported token type auth details type " + details.getClass());
            }

            return Optional.ofNullable(tokenType);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getDetailsValue(String key) {
        return getDetailsValue(key, String.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailsValue(String key, String defaultValue) {
        return getDetailsValue(key).orElse(defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getAdditionalDetailsValue(String key) {
        return getDetailsValue(XmAuthenticationConstants.AUTH_ADDITIONAL_DETAILS, HashMap.class)
                        .map(additionalDetails -> (String) additionalDetails.get(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAdditionalDetailsValue(String key, String defaultValue) {
        return getAdditionalDetailsValue(key).orElse(defaultValue);
    }

    @Override
    public Map<String, Object> getDecodedDetails() {
        return  getDetails().map(this::toDecodedDetails).orElseGet(HashMap::new);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toDecodedDetails(Object details) {
        if (details instanceof OAuth2AuthenticationDetails) {
            return Optional.of(details)
                           .map(OAuth2AuthenticationDetails.class::cast)
                           .map(OAuth2AuthenticationDetails::getDecodedDetails)
                           .map(Map.class::cast)
                           .orElseGet(Collections::emptyMap);
        } else if (details instanceof WebAuthenticationDetails) {
            return emptyMap();
        } else {
            throw new IllegalStateException("Unsupported auth details type " + details.getClass());
        }
    }

    /**
     * Deprecated - do not use in LEPs!!! Will be removed in future.
     */
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true)
    private Optional<Map<String, Object>> getDetailsMap() {
        return getDetails().flatMap(
                        details -> {
                            if (details instanceof OAuth2AuthenticationDetails) {
                                Object decodedDetails = OAuth2AuthenticationDetails.class.cast(
                                                details).getDecodedDetails();
                                return Optional.ofNullable((Map<String, Object>) decodedDetails);
                            } else if (details instanceof WebAuthenticationDetails) {
                                return Optional.empty();
                            } else {
                                throw new IllegalStateException("Unsupported auth details type "
                                                + details.getClass());
                            }
                        });
    }

    private <T> Optional<T> getDetailsValue(String key, Class<T> valueType) {
        return getDetailsMap()
                        .map(allDetail -> {
                            Object value = allDetail.get(key);
                            if (value == null) {
                                return null;
                            } else {
                                if (!valueType.isAssignableFrom(value.getClass())) {
                                    throw new IllegalArgumentException(
                                                    String.format("Can't convert detail with type %s to %s",
                                                                    value.getClass().getName(),
                                                                    valueType.getName()));
                                }
                                return valueType.cast(value);
                            }
                        });
    }

    @Override
    public String getClientId() {
        return getOAuth2Authentication()
                .map(OAuth2Authentication::getOAuth2Request)
                .map(OAuth2Request::getClientId)
                .orElse(null);
    }

    @Override
    public Set<String> getScope() {
        return getOAuth2Authentication()
                .map(OAuth2Authentication::getOAuth2Request)
                .map(OAuth2Request::getScope)
                .orElse(emptySet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthentication()
            .map(Authentication::getAuthorities)
            .orElse(emptyList());
    }

    @Override
    public Set<String> getAuthoritiesSet() {
        return getAuthentication()
            .map(Authentication::getAuthorities)
            .map(this::toAuthoritiesSet)
            .orElse(emptySet());
    }

    private Set<String> toAuthoritiesSet(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                          .map(GrantedAuthority::getAuthority)
                          .collect(toSet());
    }

}
