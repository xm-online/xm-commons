package com.icthh.xm.commons.topic.util;

import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.experimental.UtilityClass;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.StringJoiner;

import static com.icthh.xm.commons.topic.util.MessageRetryDetailsUtils.getRid;

@UtilityClass
public class MessageRetryUtils {

    public static void putRid(ConsumerRecord<?, ?> record, String tenantKey, String topicName) {
        MdcUtils.putRid(new StringJoiner(":")
            .add(tenantKey)
            .add(topicName)
            .add(getRid(record))
            .toString());
    }
}
