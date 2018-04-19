package com.icthh.xm.commons.config.client.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.config.client.exception.ConflictUpdateConfigException;
import com.icthh.xm.commons.config.client.repository.ConfigRepository;
import com.icthh.xm.commons.config.client.service.ConfigServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigConfiguration {

    public static final String XM_CONFIG_REST_TEMPLATE = "xm-config-rest-template";

    @Bean
    public ConfigRepository configRepository(@Qualifier(XM_CONFIG_REST_TEMPLATE) RestTemplate restTemplate,
                                            XmConfigProperties xmConfigProperties) {
        return new ConfigRepository(restTemplate, xmConfigProperties);
    }

    @Bean
    public ConfigServiceImpl configService(ConfigRepository configRepository) {
        return new ConfigServiceImpl(configRepository);
    }

    @Bean(XM_CONFIG_REST_TEMPLATE)
    public RestTemplate restTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
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
