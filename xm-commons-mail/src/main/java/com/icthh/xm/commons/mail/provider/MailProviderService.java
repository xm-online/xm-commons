package com.icthh.xm.commons.mail.provider;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.UNWRAP_ROOT_VALUE;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service allows to manage different Email providers per tenant using ms-config file.
 *
 * Config file location in ms-config: /config/tenants/{tenantName}/mail-provider.yml
 *
 * Config file content example (based on spring.mail application configuration):
 * <pre>
 *     mail-provider:
 *       host: localhost
 *       port: 25
 *       protocol: smtp
 *       username: mailuser
 *       password: mailpass
 *       properties:
 *           mail.smtp.starttls.enable: true
 *           ssl.trust: true
 *           mail.imap.ssl.enable: true
 * </pre>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailProviderService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final String MAIL_PROVIDER_PATH = "/config/tenants/{tenantName}/mail-config.yml";

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final Map<String, JavaMailSender> mailSenderMap = new ConcurrentHashMap<>();

    private final JavaMailSender defaultMailSender;

    @Override
    public void onInit(final String configKey, final String configValue) {
        if (this.isListeningConfiguration(configKey)) {
            this.onRefresh(configKey, configValue);
        }
    }

    @Override
    public void onRefresh(final String updatedKey, final String config) {
        try {

            String tenant = getTenantKey(updatedKey);

            if (StringUtils.isNotEmpty(config)) {

                ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.configure(UNWRAP_ROOT_VALUE, true);

                MailProviderConfig providerConfig = objectMapper.readValue(config, MailProviderConfig.class);

                mailSenderMap.put(tenant, createMailSender(providerConfig));

                log.info("Updated MailProvider for tenant {}, by path: {} with config = {}",
                         tenant, updatedKey, providerConfig);

            } else {
                mailSenderMap.remove(tenant);
                log.info("Removed MailProvider for tenant {}, by path: {}", tenant, updatedKey);
            }

        } catch (Exception e) {
            log.error("Error read tenant configuration from path [{}]", updatedKey, e);
        }

    }

    @Override
    public boolean isListeningConfiguration(final String updatedKey) {
        return this.matcher.match(MAIL_PROVIDER_PATH, updatedKey);
    }

    /**
     * Obtains @{@link JavaMailSender} instance which is tenant-specific or default.
     *
     * @param tenant tenant name
     * @return java mail sender instance.
     */
    public JavaMailSender getJavaMailSender(String tenant) {
        return mailSenderMap.getOrDefault(tenant, defaultMailSender);
    }

    /**
     * Checks if provider contains tenant specific mail sender.
     * @param tenant tenant name
     * @return true If provider has tenant specific @{@link JavaMailSender}
     */
    public boolean isTenantMailSenderExists(String tenant) {
        return mailSenderMap.containsKey(tenant);
    }

    private String getTenantKey(String updatedKey) {
        return matcher.extractUriTemplateVariables(MAIL_PROVIDER_PATH, updatedKey).get(TENANT_NAME);
    }

    private JavaMailSender createMailSender(MailProviderConfig config) {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        sender.setHost(config.getHost());
        if (config.getPort() != null) {
            sender.setPort(config.getPort());
        }
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setProtocol(config.getProtocol());

        if (config.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(config.getDefaultEncoding().name());
        }
        if (!config.getProperties().isEmpty()) {
            sender.setJavaMailProperties(asProperties(config.getProperties()));
        }
        return sender;
    }

    private Properties asProperties(Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }

    @Data
    @JsonRootName("mail-provider")
    private static class MailProviderConfig {

        private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

        private String host;
        private Integer port;
        private String username;
        private String password;
        private String protocol = "smtp";
        private Charset defaultEncoding = DEFAULT_CHARSET;
        private Map<String, String> properties = new HashMap<>();

    }

}
