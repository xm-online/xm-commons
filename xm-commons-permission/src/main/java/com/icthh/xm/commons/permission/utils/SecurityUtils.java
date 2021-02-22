package com.icthh.xm.commons.permission.utils;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static final String AUTH_ADDITIONAL_DETAILS = "additionalDetails";

    public Optional<Boolean> getAdditionalDetailsValueBoolean(Authentication authentication, String fieldName) {
        return getDetailsValue(authentication, AUTH_ADDITIONAL_DETAILS, HashMap.class)
            .map(additionalDetails -> (Boolean) additionalDetails.get(fieldName));
    }

    private <T> Optional<T> getDetailsValue(Authentication authentication, String key, Class<T> valueType) {
        return ofNullable(authentication.getDetails())
            .flatMap(this::toDetails)
            .map(allDetail -> toDetailsValue(allDetail, key, valueType));
    }

    private <T> T toDetailsValue(final Map<String, Object> allDetail, final String key, final Class<T> valueType) {
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

    private Optional<Map<String, Object>> toDetails(final Object details) {
        if (details instanceof OAuth2AuthenticationDetails) {
            Object decodedDetails = ((OAuth2AuthenticationDetails) details).getDecodedDetails();
            return ofNullable((Map<String, Object>) decodedDetails);
        } else if (details instanceof WebAuthenticationDetails) {
            return empty();
        } else {
            throw new IllegalStateException("Unsupported auth details type " + details.getClass());
        }
    }

}
