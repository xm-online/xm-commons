package com.icthh.xm.commons.security;

import java.util.Optional;

/**
 * The {@link XmAuthenticationContext} class.
 */
public interface XmAuthenticationContext {

    /**
     * Checks is context has authentication.
     *
     * @return {@code true} if security context has authentication (oAuth2 authentication) and {@code false} otherwise
     */
    boolean hasAuthentication();

    /**
     * Determines if the Authentication is anonymous.
     *
     * @return true if the user is anonymous, else false
     */
    boolean isAnonymous();

    /**
     * Determines if the Authentication is authenticated.
     *
     * @return true if the Authentication is authenticated, else false
     */
    boolean isAuthenticated();

    /**
     * Determines if the Authentication was authenticated using 'remember me'.
     *
     * @return true if the Authentication authenticated using 'remember me', else false
     */
    boolean isRememberMe();

    /**
     * Determines if the Authentication authenticated without the use of 'remember me' and not anonymous.
     *
     * @return true if the Authentication authenticated without the use of 'remember me' and not anonymous, else false
     */
    boolean isFullyAuthenticated();

    /**
     * Return login or empty optional if no authentication information is available (for unauthenticated endpoints).
     *
     * @return login optional
     */
    Optional<String> getLogin();

    /**
     * Return login or throws exception if no Authentication or Authentication sub type not supported.
     *
     * @return login value (never {@code null})
     */
    String getRequiredLogin();

    /**
     * Indicates the TCP/IP address the authentication request was received from.
     *
     * @return the address
     */
    Optional<String> getRemoteAddress();

    /**
     * Indicates the {@code HttpSession} id the authentication request was received from.
     *
     * @return the session ID
     */
    Optional<String> getSessionId();

    /**
     * Return uaa user key or empty optional if no authentication information is available.
     *
     * @return uaa user key
     */
    Optional<String> getUserKey();

    /**
     * Return userKey or throws exception if no Authentication or Authentication sub type not supported.
     *
     * @return userKey value (never {@code null})
     */
    String getRequiredUserKey();

    /**
     * The access token value used to authenticate the request (normally in an authorization header).
     *
     * @return the tokenValue used to authenticate the request
     */
    Optional<String> getTokenValue();

    /**
     * The access token type used to authenticate the request (normally in an authorization header).
     *
     * @return the tokenType used to authenticate the request if known
     */
    Optional<String> getTokenType();

    /**
     * Return 'details' value or empty optional if no authentication information is available
     * (for unauthenticated endpoints).
     *
     * @return 'details' value optional
     */
    Optional<String> getDetailsValue(String key);

    /**
     * Return 'details' value. If authentication information doesn't contain {@code key} or
     * value for key is {@code null} then return {@code defaultValue}.
     *
     * @return 'details' value or {@code defaultValue}
     */
    String getDetailsValue(String key, String defaultValue);

    /**
     * Return 'additional details' value or empty optional if no authentication information is available
     * (for unauthenticated endpoints).
     *
     * @return 'additional details' value optional
     */
    Optional<String> getAdditionalDetailsValue(String key);

    /**
     * Return 'additional details' value. If authentication information doesn't contain {@code key} or
     * value for key is {@code null} then return {@code defaultValue}.
     *
     * @return 'details' value or {@code defaultValue}
     */
    String getAdditionalDetailsValue(String key, String defaultValue);

}
