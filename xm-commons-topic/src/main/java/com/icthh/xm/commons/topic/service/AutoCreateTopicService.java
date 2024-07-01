package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.topic.config.AutoCreateTopicConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "xm-topic.auto-create.enable", matchIfMissing = false, havingValue = "true")
public class AutoCreateTopicService {

    private final KafkaProperties kafkaProperties;
    private final AutoCreateTopicConfiguration autoCreateTopicConfigs;

    @PostConstruct
    public void createTopics() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        try (AdminClient client = AdminClient.create(configs)) {
            ListTopicsOptions options = new ListTopicsOptions();
            options.listInternal(true);
            ListTopicsResult topics = client.listTopics(options);
            Set<String> currentTopicList = topics.names().get();
            for (AutoCreateTopicConfiguration.AutoCreateTopicConfig config : autoCreateTopicConfigs.getConfig()) {
                if (!currentTopicList.contains(config.getTopicName())) {
                    log.info("Creating topic {}", config.getTopicName());
                    NewTopic newTopic = new NewTopic(config.getTopicName(), config.getNumPartitions(),
                        config.getReplicationFactor().shortValue());
                    CreateTopicsResult result = client.createTopics(List.of(newTopic));
                    result.values().get(config.getTopicName()).get();
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Cannot create topics from config {}", autoCreateTopicConfigs, e);
        }
    }

}
