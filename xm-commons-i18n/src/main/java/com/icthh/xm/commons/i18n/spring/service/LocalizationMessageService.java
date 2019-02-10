package com.icthh.xm.commons.i18n.spring.service;

import static com.icthh.xm.commons.i18n.I18nConstants.LANGUAGE;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getTenantKey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.i18n.spring.config.LocalizationMessageProperties;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing i18n-message.yml per tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreLogginAspect
public class LocalizationMessageService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private final LocalizationMessageProperties localizationMessageProperties;
    private final MessageSource messageSource;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;

    /*
     * root map key is tenant name, value is map which key is message_code and value is another map
     * with key = locale and value = message_value
     */
    private final ConcurrentHashMap<String, Map<String, Map<Locale, String>>> tenantLocalizedMessageConfig = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Finds localized message template by code and current locale from config. If not found it
     * takes message from message bundle or from default message first, depends on flag.
     * @param code the message code
     * @param firstFindInMessageBundle indicates where try to find message first when config has
     *            returned NULL
     * @param defaultMessage the default message
     * @return localized message
     */
    public String getMessage(String code,
                             Map<String, String> substitutes,
                             boolean firstFindInMessageBundle,
                             String defaultMessage) {
        Locale locale = authContextHolder.getContext().getDetailsValue(LANGUAGE)
                        .map(Locale::forLanguageTag).orElse(LocaleContextHolder.getLocale());

        String localizedMessage = getFromConfig(code, locale).orElseGet(() -> {
            if (firstFindInMessageBundle) {
                return messageSource.getMessage(code, null, defaultMessage, locale);
            } else {
                return defaultMessage != null ? defaultMessage : messageSource.getMessage(code, null, locale);
            }
        });

        if (MapUtils.isNotEmpty(substitutes)) {
            localizedMessage = new StringSubstitutor(substitutes).replace(localizedMessage);
        }

        return localizedMessage;
    }

    /**
     * Finds localized message by code and current locale from config. If not found it takes message
     * from message bundle.
     * @param code the message code
     * @return localized message
     */
    public String getMessage(String code) {
        return getMessage(code, null, true, null);
    }

    /**
     * Finds localized message template by code and current locale from config. If not found it
     * takes message from message bundle and replaces all the occurrences of variables with their
     * matching values from the substitute map.
     * @param code the message code
     * @param substitutes the substitute map for message template
     * @return localized message
     */
    public String getMessage(String code, Map<String, String> substitutes) {
        return getMessage(code, substitutes, true, null);
    }

    @Override
    public void onRefresh(String key, String config) {
        String tenant = matcher.extractUriTemplateVariables(
                        localizationMessageProperties.getConfigPath(), key).get(TENANT_NAME);
        if (StringUtils.isBlank(config)) {
            tenantLocalizedMessageConfig.remove(tenant);
            log.info("Localized error messages for tenant {} was removed", tenant);
        } else {
            try {
                Map<String, Map<Locale, String>> localizedMessages = mapper.readValue(config,
                                new TypeReference<Map<String, Map<Locale, String>>>() {});
                tenantLocalizedMessageConfig.put(tenant, localizedMessages);
                log.info("Localized error messages for tenant {} was updated", tenant);
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading config by path: {}" + key, e);
            }
        }
    }

    @Override
    public boolean isListeningConfiguration(String key) {
        return matcher.match(localizationMessageProperties.getConfigPath(), key);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

    private Optional<String> getFromConfig(String messageCode, Locale locale) {

        return getTenantKey(tenantContextHolder)
            .map(TenantKey::getValue)
            .map(tenantLocalizedMessageConfig::get)
            .map(localizedMessageConfig -> localizedMessageConfig.get(messageCode))
            .map(localizedMessages -> localizedMessages.get(locale));
    }
}
