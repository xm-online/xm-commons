package com.icthh.xm.commons.scheduler.adapter;

import static com.icthh.xm.commons.config.client.repository.TenantListRepository.TENANTS_LIST_CONFIG_KEY;
import static com.icthh.xm.commons.config.client.repository.TenantListRepository.isSuspended;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.earliest;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.latest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.TenantState;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.service.SchedulerEventService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.kafka.KafkaBinderHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binder.kafka.config.KafkaBinderConfiguration;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBindingProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableBinding
@EnableIntegration
@Import(KafkaBinderConfiguration.class)
@ConditionalOnProperty("application.scheduler-enabled")
public class SchedulerChannelManager implements RefreshableConfiguration {

    private static final String PREFIX = "scheduler_";
    private static final String KAFKA = "kafka";
    private static final String DELIMITER = "_";
    private static final String GENERALGROUP = "GENERALGROUP";
    private static final String TOPIC = "_topic";
    private static final String QUEUE = "_queue";
    private static final String SCHEDULER_APP_DEFAULT = "scheduler";
    private static final String WRAP_TOKEN = "\"";

    private final BindingServiceProperties bindingServiceProperties;
    private final SubscribableChannelBindingTargetFactory bindingTargetFactory;
    private final BindingService bindingService;
    private final KafkaExtendedBindingProperties kafkaExtendedBindingProperties = new KafkaExtendedBindingProperties();
    private final Map<String, SubscribableChannel> channels = new ConcurrentHashMap<>();
    private final SchedulerEventService schedulerEventService;
    private CompositeHealthIndicator bindersHealthIndicator;
    private KafkaBinderHealthIndicator kafkaBinderHealthIndicator;

    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    String appName;

    @Value("${application.scheduler-config.scheduler-app-name:" + SCHEDULER_APP_DEFAULT + "}")
    private String schedulerAppName = SCHEDULER_APP_DEFAULT;

    @Value("${application.scheduler-config.task-back-off-initial-interval:1000}")
    private int backOffInitialInterval;

    @Value("${application.scheduler-config.task-back-off-max-interval:60000}")
    private int backOffMaxInterval;

    @Value("${application.kafka-metadata-max-age:60000}")
    private int kafkaMetadataMaxAge;

    private final Set<String> includedTenants;

    private Set<String> tenantToStart;

    @Autowired
    public SchedulerChannelManager(BindingServiceProperties bindingServiceProperties,
                                   SubscribableChannelBindingTargetFactory bindingTargetFactory,
                                   BindingService bindingService,
                                   KafkaMessageChannelBinder kafkaMessageChannelBinder,
                                   ObjectMapper objectMapper,
                                   SchedulerEventService schedulerEventService,
                                   CompositeHealthIndicator bindersHealthIndicator,
                                   KafkaBinderHealthIndicator kafkaBinderHealthIndicator,
                                   XmConfigProperties xmConfigProperties) {
        this.bindingServiceProperties = bindingServiceProperties;
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingService = bindingService;
        this.schedulerEventService = schedulerEventService;
        this.objectMapper = objectMapper;
        this.bindersHealthIndicator = bindersHealthIndicator;
        this.kafkaBinderHealthIndicator = kafkaBinderHealthIndicator;
        this.includedTenants = xmConfigProperties.getIncludeTenantLowercase();
        kafkaMessageChannelBinder.setExtendedBindingProperties(kafkaExtendedBindingProperties);
    }

    void createChannels(String tenantName) {
        try {
            String tenant = lowerCase(tenantName);
            String tenantKey = upperCase(tenantName);
            String id = randomUUID().toString();

            createHandler(PREFIX + tenant + QUEUE, GENERALGROUP, tenantKey, earliest);
            createHandler(PREFIX + tenant + TOPIC, id, tenantKey, latest);
            createHandler(PREFIX + tenant + DELIMITER + appName + QUEUE, appName, tenantKey, earliest);
            createHandler(PREFIX + tenant + DELIMITER + appName + TOPIC, id, tenantKey, latest);
        } catch (Exception e) {
            log.error("Error create scheduler channels for tenant " + tenantName, e);
            throw e;
        }
    }

    synchronized void createHandler(String chanelName, String consumerGroup, String tenantName, StartOffset startOffset) {
        if (!channels.containsKey(chanelName)) {

            log.info("Create binding to {}. Consumer group {}", chanelName, consumerGroup);

            KafkaBindingProperties props = new KafkaBindingProperties();
            props.getConsumer().setAutoCommitOffset(false);
            props.getConsumer().setStartOffset(startOffset);
            props.getConsumer().getConfiguration().put(METADATA_MAX_AGE_CONFIG, String.valueOf(kafkaMetadataMaxAge));
            kafkaExtendedBindingProperties.setBindings(Collections.singletonMap(chanelName, props));

            ConsumerProperties consumerProperties = new ConsumerProperties();
            consumerProperties.setMaxAttempts(Integer.MAX_VALUE);
            consumerProperties.setHeaderMode(HeaderMode.none);
            consumerProperties.setBackOffInitialInterval(backOffInitialInterval);
            consumerProperties.setBackOffMaxInterval(backOffMaxInterval);

            BindingProperties bindingProperties = new BindingProperties();
            bindingProperties.setConsumer(consumerProperties);
            bindingProperties.setDestination(chanelName);
            bindingProperties.setGroup(consumerGroup);
            bindingServiceProperties.getBindings().put(chanelName, bindingProperties);

            SubscribableChannel channel = bindingTargetFactory.createInput(chanelName);
            bindingService.bindConsumer(channel, chanelName);

            HealthIndicatorRegistry registry = bindersHealthIndicator.getRegistry();
            if (registry.get(KAFKA) == null) {
                bindersHealthIndicator.getRegistry().register(KAFKA, kafkaBinderHealthIndicator);
            }

            channels.put(chanelName, channel);

            channel.subscribe(message -> {
                try {
                    MdcUtils.putRid(MdcUtils.generateRid() + ":" + tenantName);
                    StopWatch stopWatch = StopWatch.createStarted();
                    String payloadString = (String) message.getPayload();
                    payloadString = unwrap(payloadString);
                    log.debug("start processing message for tenant: [{}], raw body in base64 = {}",
                              tenantName, payloadString);
                    String eventBody = new String(Base64.getDecoder().decode(payloadString), UTF_8);
                    log.info("start processing message for tenant: [{}], body = {}", tenantName, eventBody);

                    schedulerEventService.processSchedulerEvent(mapToEvent(eventBody), tenantName);

                    Optional.ofNullable(message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class))
                            .ifPresent(Acknowledgment::acknowledge);

                    log.info("stop processing message for tenant: [{}], time = {}",
                             tenantName,
                             stopWatch.getTime());
                } catch (Exception e) {
                    log.error("error processing event for tenant [{}]", tenantName, e);
                    throw e;
                } finally {
                    MdcUtils.clear();
                }
            });
        }
    }


    @SneakyThrows
    private ScheduledEvent mapToEvent(String eventBody) {
        return objectMapper.readValue(eventBody, ScheduledEvent.class);
    }

    @SneakyThrows
    void parseConfig(String key, String config) {

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

        tenantToStart = tenantKeys.stream()
                                  .filter(TenantListRepository.isIncluded(includedTenants)
                                                              .and(isSuspended().negate()))
                                  .map(TenantState::getName)
                                  .collect(Collectors.toSet());

        log.info("scheduler will be turned on for tenants: {}", tenantToStart);

    }

    // Do not delete @Async! Otherwise bindingService.bindConsumer(channel, chanelName) will initiate recursive call
    // through Spring application creation and startup event sending.
    // See: https://github.com/spring-cloud/spring-cloud-stream/issues/609
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void startChannels() {
        if (tenantToStart == null) {
            throw new IllegalStateException("Scheduler channel manager was not initialized. Call onInit() first!");
        }
        log.info("Start channels for tenants: {}", tenantToStart);
        tenantToStart.forEach(this::createChannels);
    }

    @Override
    public void onRefresh(String key, String config) {
        parseConfig(key, config);
        startChannels();
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return TENANTS_LIST_CONFIG_KEY.equals(updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        parseConfig(key, config);
    }

    private static String unwrap(final String str) {
        if (isEmpty(str) || isEmpty(WRAP_TOKEN)) {
            return str;
        }

        if (startsWith(str, WRAP_TOKEN) && endsWith(str, WRAP_TOKEN)) {
            final int startIndex = str.indexOf(WRAP_TOKEN);
            final int endIndex = str.lastIndexOf(WRAP_TOKEN);
            final int wrapLength = WRAP_TOKEN.length();
            if (startIndex != -1 && endIndex != -1) {
                return str.substring(startIndex + wrapLength, endIndex);
            }
        }

        return str;
    }

}
