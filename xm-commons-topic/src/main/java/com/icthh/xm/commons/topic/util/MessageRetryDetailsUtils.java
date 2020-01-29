package com.icthh.xm.commons.topic.util;

import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UtilityClass
public class MessageRetryDetailsUtils {

    private static ConcurrentHashMap<Record, MessageRetryDetails> retryDetails = new ConcurrentHashMap<>();

    public static MessageRetryDetails getUpdatedOrGenerateRetryDetails(ConsumerRecord<String, String> message) {
        Record record = new Record(message);
        MessageRetryDetails messageRetryDetails = retryDetails.get(record);
        log.debug("find retry details: {} for kafka message: {}", messageRetryDetails, message);

        if (messageRetryDetails == null) {
            messageRetryDetails = new MessageRetryDetails();
            messageRetryDetails.setRid(MdcUtils.generateRid());
            messageRetryDetails.setRetryCount(BigInteger.ONE);
            messageRetryDetails.setStartProcessTime(new Date().getTime());
            retryDetails.put(record, messageRetryDetails);
            log.debug("created retry details: {}, {}", record, messageRetryDetails);
        } else {
            messageRetryDetails.setRetryCount(messageRetryDetails.getRetryCount().add(BigInteger.ONE));
        }

        return messageRetryDetails;
    }

    public static BigInteger getRetryCounter(ConsumerRecord<?, ?> message) {
        Record record = new Record(message);
        return  retryDetails.get(record) == null ? null : retryDetails.get(record).getRetryCount();

    }

    public static Long getTotalProcessingTime(ConsumerRecord<?, ?> message) {
        MessageRetryDetails messageRetryDetails = retryDetails.get(new Record(message));
        return messageRetryDetails == null ? null : new Date().getTime() - messageRetryDetails.getStartProcessTime();
    }

    public static String getRid(ConsumerRecord<?, ?> message) {
        Record record = new Record(message);
        return retryDetails.get(record) == null ? null : retryDetails.get(record).getRid();
    }

    public static void delete(ConsumerRecord<?, ?> message) {
        MessageRetryDetails result = retryDetails.remove(new Record(message));
        log.debug("deleted retry details: {}, for message: {}", result, message);
    }

    @Data
    public static class MessageRetryDetails {
        private String rid;
        private BigInteger retryCount;
        private long startProcessTime;
    }

    @Data
    private static class Record {
        private String topic;
        private int partition;
        private long offset;
        private long timestamp;

        Record(ConsumerRecord<?, ?> message) {
            this.topic = message.topic();
            this.partition = message.partition();
            this.offset = message.offset();
            this.timestamp = message.timestamp();
        }
    }

}
