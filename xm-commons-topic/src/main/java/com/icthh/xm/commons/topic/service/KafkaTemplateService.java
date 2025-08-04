package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
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

    /**
     * Send the data to the provided topic with no key or partition.
     *
     * @param topic the topic.
     * @param data  The data.
     * @return a Future for the {@link SendResult}.
     **/
    @LoggingAspectConfig(inputExcludeParams = "data", resultDetails = false)
    public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
        return kafkaTemplate.send(topic, data);
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
        return kafkaTemplate.send(topic, partition, key, data);
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
        MessageBuilder<String> builder = MessageBuilder
            .withPayload(data)
            .setHeader(KafkaHeaders.TOPIC, topic)
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
