package com.icthh.xm.commons.topic.util;

import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.experimental.UtilityClass;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.math.BigInteger;
import java.util.Date;

@UtilityClass
public class MessageHeaderUtils {

    private static final String RID_HEADER = "xm.rid";
    private static final String RETRY_HEADER = "xm.retry";
    private static final String START_PROCESSING_TIME_HEADER = "xm.start.processing.time";

    public static String getOrGenerateRid(ConsumerRecord<String, String> record) {
        Headers headers = record.headers();
        Header ridHeader = headers.lastHeader(RID_HEADER);
        String rid;

        if (ridHeader == null) {
            rid = MdcUtils.generateRid();
            headers.add(RID_HEADER, rid.getBytes());
        } else {
            rid = new String(ridHeader.value());
        }

        return rid;
    }

    public static BigInteger getAndIncrementRetryCounter(ConsumerRecord<String, String> record) {
        Headers headers = record.headers();
        Header retryHeader = headers.lastHeader(RETRY_HEADER);
        BigInteger retryCount;

        if (retryHeader == null) {
            retryCount = BigInteger.ONE;
            headers.add(START_PROCESSING_TIME_HEADER, getTimestamp());
        } else {
            retryCount = new BigInteger(retryHeader.value()).add(BigInteger.ONE);
        }

        headers.add(RETRY_HEADER, retryCount.toByteArray());
        return retryCount;
    }

    public static BigInteger getRetryCounter(ConsumerRecord<?, ?> record) {
        Headers headers = record.headers();
        Header retryHeader = headers.lastHeader(RETRY_HEADER);
        return retryHeader == null ? null : new BigInteger(retryHeader.value());
    }

    public static Long getTotalProcessingTime(ConsumerRecord<?, ?> record) {
        Headers headers = record.headers();
        Header timestampHeader = headers.lastHeader(START_PROCESSING_TIME_HEADER);

        if (timestampHeader == null) {
            return null;
        }

        return new Date().getTime() - Long.parseLong(new String(timestampHeader.value()));
    }

    public static String getRid(ConsumerRecord<?, ?> record) {
        Headers headers = record.headers();
        Header ridHeader = headers.lastHeader(RID_HEADER);
        return ridHeader == null ? null : new String(ridHeader.value());
    }

    private byte[] getTimestamp() {
        return String.valueOf(new Date().getTime()).getBytes();
    }
}
