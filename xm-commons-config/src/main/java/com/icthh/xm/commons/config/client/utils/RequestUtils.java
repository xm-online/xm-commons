package com.icthh.xm.commons.config.client.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@UtilityClass
public class RequestUtils {

    public static HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = new MediaType(MediaType.TEXT_PLAIN, UTF_8);
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + TokenUtils.extractCurrentToken());
        return headers;
    }
}
