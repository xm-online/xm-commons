package com.icthh.xm.commons.topic.service;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
@RequiredArgsConstructor
@LoggingAspectConfig(resultDetails = false)
public class KafkaTemplateService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Send the data to the provided topic with no key or partition.
     *
     * @param topic the topic.
     * @param data  The data.
     * @return a Future for the {@link SendResult}.
     **/
    public ListenableFuture<SendResult<String, String>> send(String topic, String data) {
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
    public ListenableFuture<SendResult<String, String>> send(String topic,
                                                             Integer partition,
                                                             String key,
                                                             String data) {
        return kafkaTemplate.send(topic, partition, key, data);
    }
}
