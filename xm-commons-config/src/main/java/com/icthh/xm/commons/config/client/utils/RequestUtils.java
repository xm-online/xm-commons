package com.icthh.xm.commons.config.client.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@UtilityClass
public class RequestUtils {

    public static HttpHeaders createAuthHeaders() {
        return createAuthHeaders(new MediaType(MediaType.TEXT_PLAIN, UTF_8));
    }

    public static HttpHeaders createMultipartAuthHeaders() {
        return createAuthHeaders(MediaType.MULTIPART_FORM_DATA);
    }
    public static HttpHeaders createJsonAuthHeaders() {
        return createAuthHeaders(new MediaType(MediaType.APPLICATION_JSON, UTF_8));
    }

    public static HttpHeaders createSimpleHeaders() {
        return createHeaders(new MediaType(MediaType.TEXT_PLAIN, UTF_8));
    }

    public static HttpHeaders createApplicationJsonHeaders() {
        return createHeaders(new MediaType(MediaType.APPLICATION_JSON, UTF_8));
    }

    public static HttpHeaders createAuthHeaders(MediaType mediaType){
        HttpHeaders headers = createHeaders(mediaType);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + TokenUtils.extractCurrentToken());
        return headers;
    }

    public static HttpHeaders createHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        return headers;
    }
}
