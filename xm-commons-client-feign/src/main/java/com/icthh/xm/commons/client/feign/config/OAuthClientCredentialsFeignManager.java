package com.icthh.xm.commons.client.feign.config;

import static java.util.Objects.isNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@Slf4j
@RequiredArgsConstructor
public class OAuthClientCredentialsFeignManager {

    private final OAuth2AuthorizedClientManager manager;
    private final ClientRegistration clientRegistration;

    public OAuth2AccessToken getAccessToken() {
        try {
            OAuth2AuthorizeRequest oAuth2AuthorizeRequest = buildRequest();
            OAuth2AuthorizedClient client = manager.authorize(oAuth2AuthorizeRequest);
            if (isNull(client)) {
                log.error("Client is null for client registration id {}", clientRegistration.getRegistrationId());
                throw new IllegalStateException(
                    "Client credentials flow on " + clientRegistration.getRegistrationId() + " failed, client is null");
            }
            return client.getAccessToken();
        } catch (Exception exp) {
            log.error("Client credentials error, ex: ", exp);
        }
        return null;
    }

    protected OAuth2AuthorizeRequest buildRequest() {
        return OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistration.getRegistrationId())
            .principal(clientRegistration.getClientId())
            .build();
    }
}
