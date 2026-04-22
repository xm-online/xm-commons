package com.icthh.xm.commons.security.oauth2;


import java.nio.charset.StandardCharsets;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client fetching the public key from MS Config to create a SignatureVerifier.
 */
@Slf4j
public class ConfigVerificationKeyClient implements JwtVerificationKeyClient {

    private final RestTemplate restTemplate;
    private final OAuth2Properties oauth2Properties;

    public ConfigVerificationKeyClient(OAuth2Properties oauth2Properties,
                                       @Qualifier("xm-config-rest-template") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.oauth2Properties = oauth2Properties;
    }

    /**
     * Fetches the public key from the MS Config.
     *
     * @return the public key used to verify JWT tokens; or null.
     */
    @Override
    public byte[] fetchKeyContent() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String stringContent = restTemplate.exchange(getPublicKeyEndpoint(),
                HttpMethod.GET, request, String.class).getBody();

            if (StringUtils.isBlank(stringContent)) {
                log.warn("Public key not fetched from endpoint: {}", getPublicKeyEndpoint());
                return null;
            }

            return stringContent.getBytes(StandardCharsets.UTF_8);
        } catch (RestClientException ex) {
            log.error("Could not contact xm-ms-config to get public key, exception: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Returns the configured endpoint URI to retrieve the public key.
     */
    private String getPublicKeyEndpoint() {
        String tokenEndpointUrl = oauth2Properties.getSignatureVerification().getPublicKeyEndpointUri();
        if (tokenEndpointUrl == null) {
            throw new IllegalStateException("no token endpoint configured in application properties");
        }
        return tokenEndpointUrl;
    }
}
