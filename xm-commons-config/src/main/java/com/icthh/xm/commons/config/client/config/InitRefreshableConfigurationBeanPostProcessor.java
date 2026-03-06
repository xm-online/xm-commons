package com.icthh.xm.commons.config.client.config;

import static org.apache.commons.lang3.StringUtils.length;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.ConfigurationChangedListener;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InitRefreshableConfigurationBeanPostProcessor implements BeanPostProcessor {

    public static final String LOG_CONFIG_EMPTY = "<CONFIG_EMPTY>";
    private static final String CONFIG_PATH = "/config/tenants/{tenantName}/**";
    private static final String COMMONS = "commons";
    private final ObjectProvider<ConfigService> configServiceProvider;

    private final Map<String, RefreshableConfiguration> refreshableConfigurations = new HashMap<>();
    private volatile Map<String, Configuration> configMap;

    private final Set<String> includedTenants;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final FetchConfigurationSettings fetchConfigurationSettings;

    public InitRefreshableConfigurationBeanPostProcessor(ObjectProvider<ConfigService> configServiceProvider,
                                                         XmConfigProperties xmConfigProperties,
                                                         FetchConfigurationSettings fetchConfigurationSettings) {
        this.configServiceProvider = configServiceProvider;
        this.includedTenants = xmConfigProperties.getIncludeTenantUppercase();
        this.fetchConfigurationSettings = fetchConfigurationSettings;
        addLepCommons();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RefreshableConfiguration) {
            refreshableConfigurations.put(beanName, (RefreshableConfiguration) bean);
            log.info("refreshable configuration bean added: {} = {}", beanName, bean.getClass());
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (refreshableConfigurations.containsKey(beanName)) {
            initBean(refreshableConfigurations.get(beanName), getConfig());
        }
        return bean;
    }

    private Map<String, Configuration> getConfig() {
        if (configMap == null) {
            configMap = getConfigService().getConfigMapAntPattern(null, fetchConfigurationSettings.getMsConfigPatterns());
        }
        return configMap;
    }

    private void initBean(RefreshableConfiguration refreshableConfiguration, Map<String, Configuration> configMap) {
        List<String> initedPaths = initConfigPaths(refreshableConfiguration, configMap);
        refreshFinished(refreshableConfiguration, initedPaths);

        log.info("refreshable configuration bean [{}] initialized by configMap with {} entries",
            getBeanName(refreshableConfiguration), configMap.size());

        getConfigService().addConfigurationChangedListener(new ConfigurationChangedListener() {
            @Override
            public void onConfigurationChanged(Configuration configuration) {
                onEntryChange(refreshableConfiguration, configuration);
            }

            @Override
            public void refreshFinished(Collection<String> paths) {
                List<String> listenPaths = paths.stream()
                                                .filter(s -> isTenantIncluded(s))
                                                .filter(refreshableConfiguration::isListeningConfiguration)
                                                .collect(Collectors.toList());
                if (!listenPaths.isEmpty()) {
                    InitRefreshableConfigurationBeanPostProcessor.this.refreshFinished(refreshableConfiguration, listenPaths);
                }
            }
        });

        refreshableConfiguration.refreshableConfigurationInited();
    }

    public List<String> initConfigPaths(final RefreshableConfiguration refreshableConfiguration,
                                        final Map<String, Configuration> configMap) {
        return configMap
            .entrySet()
            .stream()
            .filter(e -> isTenantIncluded(e.getKey()))
            .filter(e -> refreshableConfiguration.isListeningConfiguration(e.getKey()))
            .peek(e -> printLog(getBeanName(refreshableConfiguration), e))
            .peek(e -> refreshableConfiguration.onInit(e.getKey(), e.getValue().getContent()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private static void printLog(final String beanName,
                                 final Map.Entry<String, Configuration> e) {
        log.info("Process config init event: [key = {}, size = {}, newHash = {}] in bean: [{}]",
                 e.getKey(),
                 length(e.getValue().getContent()),
                 getValueHash(e.getValue().getContent()),
                 beanName);
    }

    private boolean isTenantIncluded(String configKey) {
        if (!includedTenants.isEmpty()) {
            if (matcher.match(CONFIG_PATH, configKey)) {
                String tenant = matcher.extractUriTemplateVariables(CONFIG_PATH, configKey).get("tenantName");
                return includedTenants.contains(tenant);
            }
            return false;
        }
        return true;
    }

    private void addLepCommons() {
        if (!includedTenants.isEmpty()) {
            includedTenants.add(COMMONS); // tenant level commons are always included.
        }
    }

    private void refreshFinished(RefreshableConfiguration refreshableConfiguration, Collection<String> paths) {
        try {
            refreshableConfiguration.refreshFinished(paths);
        } catch (Exception e) {
            log.error("Error during refresh finished", e);
        }
    }

    private void onEntryChange(RefreshableConfiguration refreshableConfiguration, Configuration configuration) {
        String configContent = configuration.getContent();

        if (isTenantIncluded(configuration.getPath())) {
            if (refreshableConfiguration.isListeningConfiguration(configuration.getPath())) {
                refreshableConfiguration.onRefresh(configuration.getPath(), configContent);

                log.info(
                    "Process config update event: [path = {}, size = {}, hash = {}] in bean: [{}]",
                    configuration.getPath(),
                    length(configContent),
                    getValueHash(configContent),
                    getBeanName(refreshableConfiguration));
            } else {
                log.debug("Ignored config update event: [path = {}, configSize = {} in bean [{}]",
                          configuration.getPath(),
                          length(configContent),
                          getBeanName(refreshableConfiguration));
            }
        }
    }

    private static String getBeanName(final RefreshableConfiguration refreshableConfiguration) {
        return refreshableConfiguration.getClass().getSimpleName();
    }

    private static String getValueHash(final String configContent) {
        return StringUtils.isEmpty(configContent) ? LOG_CONFIG_EMPTY :
               DigestUtils.md5Hex(configContent);
    }

    private ConfigService getConfigService() {
        return Optional.ofNullable(configServiceProvider.getIfAvailable())
            .orElseThrow(() -> new IllegalStateException("ConfigService is not available"));
    }
}
