package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.security.oauth2.ConfigVerificationKeyClient;
import com.icthh.xm.commons.security.oauth2.FileVerificationKeyClient;
import com.icthh.xm.commons.security.oauth2.JwtVerificationKeyClient;
import com.icthh.xm.commons.security.oauth2.OAuth2Properties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class XmAuthenticationVerificationConfiguration {

    @Bean
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "XM_MS_CONFIG", matchIfMissing = true)
    public JwtVerificationKeyClient configJwtVerificationKeyClient(OAuth2Properties oAuth2Properties,
                                                                   @Qualifier("xm-config-rest-template") RestTemplate restTemplate) {
        return new ConfigVerificationKeyClient(oAuth2Properties, restTemplate);
    }

    @Bean
    @ConditionalOnProperty(value = "xm-config.configMode", havingValue = "FILE")
    public JwtVerificationKeyClient fileJwtVerificationKeyClient(@Value("${xm-config.configDirPath}") String configDirPath) {
        return new FileVerificationKeyClient(configDirPath);
    }

}
