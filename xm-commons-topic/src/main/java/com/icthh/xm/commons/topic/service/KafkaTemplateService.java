package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.topic.config.KafkaTopicNameHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KafkaTemplateService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicNameHandler kafkaTopicNameHandler;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Send the data to the provided topic with no key or partition.
     *
     * @param topic the topic.
     * @param data  The data.
     * @return a Future for the {@link SendResult}.
     **/
    @LoggingAspectConfig(inputExcludeParams = "data", resultDetails = false)
    public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName(topic, tenantContextHolder.getTenantKey());
        return kafkaTemplate.send(prefixedTopic, data);
    }

    /**
     * Send the data to the provided topic with no key or partition (with tenant prefix support).
     *
     * @param topic  the topic.
     * @param data   The data.
     * @param tenant the tenant key for prefix
     * @return a Future for the {@link SendResult}.
     **/
    @LoggingAspectConfig(inputExcludeParams = "data", resultDetails = false)
    public CompletableFuture<SendResult<String, String>> send(String topic, String data, String tenant) {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName(topic, tenant);
        return kafkaTemplate.send(prefixedTopic, data);
    }

    /**
     * Send the data to the provided topic with the provided key and partition.
     *
     * @param topic     the topic.
     * @param partition the partition.
     * @param key       the key.
     * @param data      the data.
     * @return a Future for the {@link SendResult}.
     */
    @LoggingAspectConfig(inputExcludeParams = "data", resultDetails = false)
    public CompletableFuture<SendResult<String, String>> send(String topic,
                                                             Integer partition,
                                                             String key,
                                                             String data) {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName(topic, tenantContextHolder.getTenantKey());
        return kafkaTemplate.send(prefixedTopic, partition, key, data);
    }

    /**
     * Send the data to the provided topic with headers, key and partition.
     *
     * @param topic     the topic.
     * @param data      The data.
     * @param headers   The headers that will be included in the record
     * @return a Future for the {@link SendResult}.
     **/
    @LoggingAspectConfig(inputExcludeParams = "data", resultDetails = false)
    public CompletableFuture<SendResult<String, String>> send(String topic,
                                                              String data,
                                                              Map<String, Object> headers) {
        return send(topic, null, null, data, headers);
    }

    /**
     * Send the data to the provided topic with headers and missing key or partition.
     *
     * @param topic     the topic.
     * @param partition the partition.
     * @param key       the key.
     * @param data      The data.
     * @param headers   The headers that will be included in the record
     * @return a Future for the {@link SendResult}.
     **/
    public CompletableFuture<SendResult<String, String>> send(String topic,
                                                              Integer partition,
                                                              String key,
                                                              String data,
                                                              Map<String, Object> headers) {
        String prefixedTopic = kafkaTopicNameHandler.getPrefixedTopicName(topic, tenantContextHolder.getTenantKey());
        MessageBuilder<String> builder = MessageBuilder
            .withPayload(data)
            .setHeader(KafkaHeaders.TOPIC, prefixedTopic)
            .setHeader(KafkaHeaders.KEY, key);

        if (partition != null) {
            builder.setHeader(KafkaHeaders.PARTITION, partition);
        }

        if (headers != null) {
            builder.copyHeaders(headers);
        }

        Message<String> message = builder.build();
        return kafkaTemplate.send(message);
    }

}
