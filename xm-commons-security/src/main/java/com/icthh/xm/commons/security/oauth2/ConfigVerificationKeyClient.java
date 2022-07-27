package com.icthh.xm.commons.security.oauth2;


import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client fetching the public key from MS Config to create a SignatureVerifier.
 */
@Component
public class ConfigVerificationKeyClient implements JwtVerificationKeyClient {

    private final Logger log = LoggerFactory.getLogger(ConfigVerificationKeyClient.class);
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
            HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

            byte[] content = restTemplate.exchange(getPublicKeyEndpoint(),
                HttpMethod.GET, request, byte[].class).getBody();

            if (ArrayUtils.isEmpty(content)) {
                log.info("Public key not fetched");
                return null;
            }

            return content;
        } catch (IllegalArgumentException ex) {
            log.warn("Could not contact xm-ms-config to get public key, exception: {}", ex.getMessage());
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
