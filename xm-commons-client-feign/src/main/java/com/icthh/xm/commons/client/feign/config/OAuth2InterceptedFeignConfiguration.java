package com.icthh.xm.commons.client.feign.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestHeadersConverter;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@Configuration
@RequiredArgsConstructor
@EnableFeignClients
@Import(FeignClientsConfiguration.class)
public class OAuth2InterceptedFeignConfiguration {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String UAA_REGISTRATION_ID = "uaa";

    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final TenantContextHolder tenantContextHolder;

    @Bean
    public RequestInterceptor requestInterceptor() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(
            UAA_REGISTRATION_ID);
        OAuthClientCredentialsFeignManager clientCredentialsFeignManager =
            new OAuthClientCredentialsFeignManager(authorizedClientManager(), clientRegistration);
        return requestTemplate -> {
            OAuth2AccessToken accessToken = clientCredentialsFeignManager.getAccessToken();
            requestTemplate
                .header(AUTHORIZATION_HEADER_NAME,
                    String.join(" ", accessToken.getTokenType().getValue(),
                        accessToken.getTokenValue()));
        };
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager() {
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials(this::oAuth2AuthorizedClientProvider)
            .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
                oAuth2AuthorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    private void oAuth2AuthorizedClientProvider(
        ClientCredentialsGrantBuilder clientCredentialsGrantBuilder) {
        clientCredentialsGrantBuilder.accessTokenResponseClient(accessTokenResponseClient());
    }

    @Bean
    public RestClientClientCredentialsTokenResponseClient accessTokenResponseClient() {
        RestClientClientCredentialsTokenResponseClient accessTokenResponseClient =
            new RestClientClientCredentialsTokenResponseClient();

        accessTokenResponseClient.addHeadersConverter(
            new TenantAwareGrantRequestHeadersConverter(tenantContextHolder,
                new DefaultOAuth2TokenRequestHeadersConverter<>()));

        return accessTokenResponseClient;
    }
}
