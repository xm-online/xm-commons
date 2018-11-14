package com.icthh.xm.commons.security.oauth2;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client fetching the public key from MS Config to create a SignatureVerifier.
 */
public class ConfigSignatureVerifierClient implements OAuth2SignatureVerifierClient {

    private static final String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----";

    private final Logger log = LoggerFactory.getLogger(ConfigSignatureVerifierClient.class);
    private final RestTemplate restTemplate;
    private final OAuth2Properties oauth2Properties;

    public ConfigSignatureVerifierClient(OAuth2Properties oauth2Properties,
                                         RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.oauth2Properties = oauth2Properties;
    }

    /**
     * Fetches the public key from the MS Config.
     *
     * @return the public key used to verify JWT tokens; or null.
     */
    @Override
    public SignatureVerifier getSignatureVerifier() throws Exception {
        try {
            HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

            String content = restTemplate.exchange(getPublicKeyEndpoint(),
                HttpMethod.GET, request, String.class).getBody();

            if (StringUtils.isEmpty(content)) {
                log.info("Public key not fetched");
                return null;
            }

            InputStream fin = new ByteArrayInputStream(content.getBytes());
            CertificateFactory f = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) f.generateCertificate(fin);
            PublicKey pk = certificate.getPublicKey();

            return new RsaVerifier(String.format(PUBLIC_KEY, new String(Base64.getEncoder().encode(pk.getEncoded()))));
        } catch (IllegalStateException ex) {
            log.warn("could not contact Config to get public key");
            return null;
        }
    }

    /**
     * Returns the configured endpoint URI to retrieve the public key.
     */
    private String getPublicKeyEndpoint() {
        String tokenEndpointUrl = oauth2Properties.getSignatureVerification().getPublicKeyEndpointUri();
        if (tokenEndpointUrl == null) {
            throw new InvalidClientException("no token endpoint configured in application properties");
        }
        return tokenEndpointUrl;
    }
}
