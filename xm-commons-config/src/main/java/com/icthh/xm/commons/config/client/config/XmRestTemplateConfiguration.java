package com.icthh.xm.commons.config.client.config;

import com.icthh.xm.commons.config.client.exception.ConflictUpdateConfigException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty("xm-config.enabled")
public class XmRestTemplateConfiguration {

    public static final String XM_CONFIG_REST_TEMPLATE = "xm-config-rest-template";

    @Configuration
    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
    static class XmLoadBalancerRestTemplateConfiguration {

        @Bean(XM_CONFIG_REST_TEMPLATE)
        public RestTemplate restTemplate(RestTemplateCustomizer customizer) {
            RestTemplate restTemplate = createRestTemplate();
            customizer.customize(restTemplate);
            return restTemplate;
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "false")
    static class XmPlainRestTemplateConfiguration {

        @Bean(XM_CONFIG_REST_TEMPLATE)
        public RestTemplate restTemplate() {
            return createRestTemplate();
        }
    }

    private static RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
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
