package com.icthh.xm.commons.scheduler.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.TenantState;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.config.client.repository.TenantListRepository.TENANTS_LIST_CONFIG_KEY;
import static com.icthh.xm.commons.config.client.repository.TenantListRepository.isSuspended;

@Slf4j
public class SchedulerChannelManager implements RefreshableConfiguration {

    private static final String SCHEDULER_APP_DEFAULT = "scheduler";

    private final DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    String appName;

    @Value("${application.scheduler-config.scheduler-app-name:" + SCHEDULER_APP_DEFAULT + "}")
    private String schedulerAppName = SCHEDULER_APP_DEFAULT;

    private final Set<String> includedTenants;

    private volatile Set<String> tenantToStart;

    public SchedulerChannelManager(XmConfigProperties xmConfigProperties,
                                   DynamicTopicConsumerConfiguration dynamicTopicConsumerConfiguration) {
        this.includedTenants = xmConfigProperties.getIncludeTenantLowercase();
        this.dynamicTopicConsumerConfiguration = dynamicTopicConsumerConfiguration;
        this.objectMapper = new ObjectMapper();
    }

    @SneakyThrows
    boolean parseConfig(String key, String config) {

        log.info("Tenants list was updated, start to parse config");

        if (!TENANTS_LIST_CONFIG_KEY.equals(key)) {
            throw new IllegalArgumentException("Wrong config key to update " + key);
        }

        if (StringUtils.isEmpty(config)) {
            throw new IllegalArgumentException("Config file has empty content: " + key);
        }

        Set<TenantState> tenantKeys = TenantListRepository.parseTenantStates(config, objectMapper)
                                                          .getOrDefault(schedulerAppName, new HashSet<>());

        if (tenantKeys.isEmpty()) {
            log.warn("No one tenant configured to use scheduler. "
                     + "Add tenant state to ms-config/tenant-list.json to section $.scheduler");
        }
        if (!includedTenants.isEmpty()) {
            log.warn("Tenant list was overridden by property 'xm-config.include-tenants' to: {}", includedTenants);
        }

        var tenantToStart = tenantKeys.stream()
                                  .filter(TenantListRepository.isIncluded(includedTenants)
                                                              .and(isSuspended().negate()))
                                  .map(TenantState::getName)
                                  .collect(Collectors.toSet());

        if (Objects.equals(this.tenantToStart, tenantToStart)) {
            log.info("Tenants list was not changed old: {}, new: {}, skip update", this.tenantToStart, tenantToStart);
            return false;
        }

        this.tenantToStart = Set.copyOf(tenantToStart);
        log.info("scheduler will be turned on for tenants: {}", tenantToStart);
        return true;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startChannels() {
        if (tenantToStart == null) {
            throw new IllegalStateException("Scheduler channel manager was not initialized. Call onInit() first!");
        }
        log.info("Start channels for tenants: {}", tenantToStart);
        tenantToStart.forEach(tenantKey -> {
            dynamicTopicConsumerConfiguration.buildDynamicConsumers(tenantKey);
            dynamicTopicConsumerConfiguration.sendRefreshDynamicConsumersEvent(tenantKey);
        });
    }

    @Override
    public void onRefresh(String key, String config) {
        if (parseConfig(key, config)) {
            startChannels();
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return TENANTS_LIST_CONFIG_KEY.equals(updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        parseConfig(key, config);
    }
}
