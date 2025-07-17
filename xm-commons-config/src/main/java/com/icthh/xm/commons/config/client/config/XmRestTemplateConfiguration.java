package com.icthh.xm.commons.config.client.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.config.client.exception.ConflictUpdateConfigException;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty("xm-config.enabled")
@EnableConfigurationProperties({XmTimeoutProperties.class})
public class XmRestTemplateConfiguration {

    public static final String XM_CONFIG_REST_TEMPLATE = "xm-config-rest-template";

    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
    static class XmLoadBalancerRestTemplateConfiguration {

        @Bean(XM_CONFIG_REST_TEMPLATE)
        public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder,
            RestTemplateCustomizer customizer,
            ObjectProvider<XmTimeoutProperties> xmTimeoutPropertiesProvider) {
            log.info("restTemplate with loadBalancer");
            RestTemplate restTemplate = createRestTemplate(restTemplateBuilder, xmTimeoutPropertiesProvider);
            customizer.customize(restTemplate);
            return restTemplate;
        }
    }

    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "false")
    static class XmPlainRestTemplateConfiguration {

        @Bean(XM_CONFIG_REST_TEMPLATE)
        public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder,
            ObjectProvider<XmTimeoutProperties> xmTimeoutPropertiesProvider) {
            log.info("restTemplate without loadBalancer");
            return createRestTemplate(restTemplateBuilder, xmTimeoutPropertiesProvider);
        }
    }

    private static RestTemplate createRestTemplate(RestTemplateBuilder restTemplateBuilder,
        ObjectProvider<XmTimeoutProperties> xmTimeoutPropertiesProvider) {
        XmTimeoutProperties timeoutProperties = xmTimeoutPropertiesProvider.getIfAvailable();

        if (Objects.nonNull(timeoutProperties) &&
            ObjectUtils.anyNotNull(timeoutProperties.getConnectionTimeout(), timeoutProperties.getReadTimeout())) {

            log.info("createRestTemplate applying timeouts={}", timeoutProperties);
            restTemplateBuilder
                .setConnectTimeout(timeoutProperties.getConnectionTimeout())
                .setReadTimeout(timeoutProperties.getReadTimeout());
        }

        RestTemplate restTemplate = restTemplateBuilder.build();

        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(UTF_8));
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if (response.getStatusCode() == HttpStatus.CONFLICT) {
                    throw new ConflictUpdateConfigException();
                } else {
                    super.handleError(response);
                }
            }
        });
        return restTemplate;
    }
}
