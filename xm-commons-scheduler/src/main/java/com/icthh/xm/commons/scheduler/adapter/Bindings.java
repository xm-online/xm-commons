package com.icthh.xm.commons.scheduler.adapter;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.icthh.xm.commons.config.client.repository.TenantListRepository.TENANTS_LIST_CONFIG_KEY;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.earliest;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.latest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.domain.TenantState;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBindingProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.BindingTargetFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableBinding
@RequiredArgsConstructor
public class Bindings implements RefreshableConfiguration {

    private static final String PREFIX = "scheduler_";
    private static final String DELIMITER = "_";
    private static final String GENERALGROUP = "GENERALGROUP";
    private static final String TOPIC = "_topic";
    private static final String QUEUE = "_queue";

    private final BindingServiceProperties bindingServiceProperties;
    private final BindingTargetFactory bindingTargetFactory;
    private final BindingService bindingService;
    private final KafkaExtendedBindingProperties kafkaExtendedBindingProperties = new KafkaExtendedBindingProperties();
    private final Map<String, SubscribableChannel> channels = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.application.name}")
    private String appName;

    public Bindings(BindingServiceProperties bindingServiceProperties,
                                        BindingTargetFactory bindingTargetFactory,
                                        BindingService bindingService,
                                        KafkaMessageChannelBinder kafkaMessageChannelBinder) {
        this.bindingServiceProperties = bindingServiceProperties;
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingService = bindingService;
        kafkaMessageChannelBinder.setExtendedBindingProperties(kafkaExtendedBindingProperties);
    }

    private void createChannels(String tenantName) {
        try {
            String tenant = lowerCase(tenantName);
            createHandler(PREFIX + tenant + QUEUE, GENERALGROUP, earliest);
            createHandler(PREFIX + tenant + TOPIC, UUID.randomUUID().toString(), latest);
            createHandler(PREFIX + tenant + DELIMITER + appName + QUEUE, appName, earliest);
            createHandler(PREFIX + tenant + DELIMITER + appName + TOPIC, UUID.randomUUID().toString(), latest);
        } catch (Exception e) {
            log.error("Error create scheduler channels for tenant " + tenantName, e);
            throw e;
        }
    }

    private synchronized void createHandler(String chanelName, String consumerGroup, StartOffset startOffset) {
        if (!channels.containsKey(chanelName)) {

            KafkaBindingProperties props = new KafkaBindingProperties();
            props.getConsumer().setAutoCommitOffset(false);
            props.getConsumer().setStartOffset(startOffset);
            kafkaExtendedBindingProperties.getBindings().put(chanelName, props);

            ConsumerProperties consumerProperties = new ConsumerProperties();
            consumerProperties.setMaxAttempts(Integer.MAX_VALUE);
            BindingProperties bindingProperties = new BindingProperties();
            bindingProperties.setConsumer(consumerProperties);
            bindingProperties.setDestination(chanelName);
            bindingProperties.setGroup(consumerGroup);
            bindingServiceProperties.getBindings().put(chanelName, bindingProperties);

            SubscribableChannel channel = (SubscribableChannel) bindingTargetFactory.createInput(chanelName);
            bindingService.bindConsumer(channel, chanelName);

            channels.put(chanelName, channel);

            channel.subscribe(message -> {
                // TODO add message handler
            });
        }
    }

    @SneakyThrows
    private void updateTenants(String key, String config) {
        log.info("Tenants list was updated");

        if (!TENANTS_LIST_CONFIG_KEY.equals(key)) {
            throw new IllegalArgumentException("Wrong config key to update " + key);
        }

        CollectionType setType = defaultInstance().constructCollectionType(HashSet.class, TenantState.class);
        MapType type = defaultInstance().constructMapType(HashMap.class, defaultInstance().constructType(String.class), setType);
        Map<String, Set<TenantState>> tenantsByServiceMap = objectMapper.readValue(config, type);
        Set<TenantState> tenantKeys = tenantsByServiceMap.get(appName);

        tenantKeys.stream().map(TenantState::getName).forEach(this::createChannels);
    }

    @Override
    public void onRefresh(String key, String config) {
        updateTenants(key, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return TENANTS_LIST_CONFIG_KEY.equals(updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        updateTenants(key, config);
    }
}
