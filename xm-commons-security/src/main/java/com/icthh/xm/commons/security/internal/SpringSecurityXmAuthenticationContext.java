package com.icthh.xm.commons.security.internal;

import com.icthh.xm.commons.security.XmAuthenticationConstants;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

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

    public Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(securityContext.getAuthentication());
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
            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }

            return null;
        });
    }

    @Override
    public Optional<String> getClientId() {
        return xmAuthenticationDetails().map(XmAuthenticationDetails::getClientId);
    }

    @Override
    public String getRequiredClientId() {
        return getClientId()
                .orElseThrow(() -> new IllegalStateException("Can't get clientId from authentication"));
    }

    @Override
    public Set<String> getScope() {
        return xmAuthenticationDetails().map(XmAuthenticationDetails::getScope).orElse(emptySet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthentication().map(Authentication::getAuthorities).orElse(emptyList());
    }

    @Override
    public Set<String> getAuthoritiesSet() {
        return xmAuthenticationDetails().map(XmAuthenticationDetails::getAuthorities).orElse(emptySet());
    }

    // DON'T USE IN LEP
    private Optional<XmAuthenticationDetails> xmAuthenticationDetails() {
        return getAuthentication().map(Authentication::getDetails)
                .filter(it -> it instanceof XmAuthenticationDetails).map(XmAuthenticationDetails.class::cast);
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
            if (details instanceof XmAuthenticationDetails xmDetails) {
                address = xmDetails.getRemoteAddress();
            } else if (details instanceof WebAuthenticationDetails webAuthenticationDetails) {
                address = webAuthenticationDetails.getRemoteAddress();
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
            if (details instanceof XmAuthenticationDetails xmDetails) {
                sessionId = xmDetails.getSessionId();
            } else if (details instanceof WebAuthenticationDetails webAuthenticationDetails) {
                sessionId = webAuthenticationDetails.getSessionId();
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
            if (details instanceof XmAuthenticationDetails xmDetails) {
                tokenValue = xmDetails.getTokenValue();
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
            if (details instanceof XmAuthenticationDetails xmDetails) {
                tokenType = xmDetails.getTokenType();
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

    @Override
    public Optional<String> getTenantName() {
        return getDetailsValue(XmAuthenticationConstants.AUTH_TENANT_KEY);
    }

    private Map<String, Object> toDecodedDetails(Object details) {
        if (details instanceof XmAuthenticationDetails xmDetails) {
            return xmDetails.getDecodedDetails();
        } else if (details instanceof WebAuthenticationDetails) {
            return null;
        } else {
            throw new IllegalStateException("Unsupported auth details type " + details.getClass());
        }
    }

    /**
     * @deprecated use getDecodedDetails instead
     */
    @Deprecated(forRemoval = true)
    private Optional<Map<String, Object>> getDetailsMap() {
        return Optional.of(getDecodedDetails());
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
}
