package com.icthh.xm.commons.permission.utils;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@UtilityClass
public class SecurityUtils {

    public static final String AUTH_ADDITIONAL_DETAILS = "additionalDetails";

    public static boolean getAdditionalDetailsValueBoolean(Authentication authentication, String fieldName) {
        return getDetailsValue(authentication, AUTH_ADDITIONAL_DETAILS, HashMap.class)
            .map(additionalDetails ->  additionalDetails.get(fieldName))
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(false);
    }

    private static <T> Optional<T> getDetailsValue(Authentication authentication, String key, Class<T> valueType) {
        return ofNullable(authentication)
            .map(Authentication::getDetails)
            .map(SecurityUtils::toDetailsMap)
            .map(allDetail -> toDetailsValue(allDetail, key, valueType));
    }

    private static <T> T toDetailsValue(final Map<?, ?> allDetail, final String key, final Class<T> valueType) {
        Object value = allDetail.get(key);
        if (isNull(value)) {
            return null;
        } else {
            if (!valueType.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException(
                    format(
                        "Can't convert detail with type %s to %s",
                        value.getClass().getName(),
                        valueType.getName()
                    )
                );
            }
            return valueType.cast(value);
        }
    }

    private static Map<?, ?> toDetailsMap(final Object details) {
        if (details instanceof XmAuthenticationDetails) {
            return of(details)
                .map(XmAuthenticationDetails.class::cast)
                .map(XmAuthenticationDetails::getDecodedDetails)
                .orElseGet(Collections::emptyMap);
        } else if (details instanceof WebAuthenticationDetails) {
            return Collections.emptyMap();
        } else {
            throw new IllegalStateException("Unsupported auth details type " + details.getClass());
        }
    }

    public static String getUserKeyOrNull(XmAuthenticationContextHolder contextHolder) {
        return Optional.ofNullable(contextHolder)
            .map(XmAuthenticationContextHolder::getContext)
            .flatMap(XmAuthenticationContext::getUserKey)
            .orElse(null);
    }

    public static String getRoleKeyOrNull(Authentication authentication) {
        return Optional.ofNullable(authentication)
            .map(Authentication::getAuthorities)
            .map(Collection::stream)
            .flatMap(Stream::findFirst)
            .map(GrantedAuthority::getAuthority)
            .orElse(null);
    }

}
