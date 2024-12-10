package com.icthh.xm.commons.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

/**
 * Utility class for HTTP headers creation.
 */
@UtilityClass
public class HeaderUtils {

    public static final String X_APP_ALERT_TEMPLATE = "X-%s-alert";
    public static final String X_APP_PARAMS_TEMPLATE = "X-%s-params";

    public static HttpHeaders createEntityCreationAlert(String appName, String entityName, String param) {
        return createAlert(appName, appName + "." + entityName + ".created", param);
    }

    public static HttpHeaders createAlert(String appName, String message, String param) {
        HttpHeaders headers = new HttpHeaders();
        // FIXME @amedvedchuk "X-" for custom headers is deprecated https://tools.ietf.org/html/rfc6648
        // See: com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME
        headers.add(String.format(X_APP_ALERT_TEMPLATE, appName), message);
        headers.add(String.format(X_APP_PARAMS_TEMPLATE, appName), param);
        return headers;
    }
}
