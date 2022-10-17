package com.icthh.xm.commons.client.feign.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@RunWith(MockitoJUnitRunner.class)
public class OAuthClientCredentialsFeignManagerUnitTest {

    private OAuthClientCredentialsFeignManager testedInstance;

    @Mock
    private OAuth2AuthorizedClientManager manager;
    @Mock
    private OAuth2AuthorizedClient oAuth2AuthorizedClient;
    @Mock
    private OAuth2AccessToken token;

    @Before
    public void setUp() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(
                "registration_id")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("token.uri")
            .clientId("client_id")
            .build();

        testedInstance = new OAuthClientCredentialsFeignManager(manager,
            clientRegistration);
    }

    @Test
    public void shouldReturnClientAccessTokenWhenRequestSuccess() {
        when(manager.authorize(any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(token);

        OAuth2AccessToken actualToken = testedInstance.getAccessToken();

        assertEquals(token, actualToken);
    }

    @Test
    public void shouldReturnNullWhenManagerNotReturnClientToGetAccessToken() {
        when(manager.authorize(any())).thenReturn(null);

        OAuth2AccessToken accessToken = testedInstance.getAccessToken();

        assertNull(accessToken);
    }

    @Test
    public void shouldReturnNullWhenGetAccessTokenRaiseAnException() {
        when(manager.authorize(any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenThrow(RuntimeException.class);

        OAuth2AccessToken accessToken = testedInstance.getAccessToken();

        assertNull(accessToken);
    }
}
