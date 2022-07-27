package com.icthh.xm.commons.config.client.utils;

import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class TokenUtils {

    public static String extractCurrentToken() {
        final XmAuthentication auth = getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("Cannot get current authentication object");
        }
        XmAuthenticationDetails details = auth.getDetails();
        if (details == null) {
            return null;
        }
        return details.getTokenValue();
    }

    private static XmAuthentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof XmAuthentication xmAuthentication) {
            return xmAuthentication;
        }
        return null;
    }

}
