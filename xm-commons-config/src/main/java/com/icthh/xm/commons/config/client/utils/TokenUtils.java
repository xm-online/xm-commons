package com.icthh.xm.commons.config.client.utils;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

@UtilityClass
public class TokenUtils {

    public static String extractCurrentToken() {
        final OAuth2Authentication auth = getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("Cannot get current authentication object");
        }
        if (auth.getDetails() == null) {
            return null;
        }
        if (auth.getDetails() instanceof OAuth2AuthenticationDetails) {
            return (OAuth2AuthenticationDetails.class.cast(auth.getDetails())).getTokenValue();
        }
        if (auth.getDetails() instanceof String) {
            return String.valueOf(auth.getDetails());
        }
        return null;
    }

    private static OAuth2Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2Authentication) {
            return (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        }
        return null;
    }

}
