package com.icthh.xm.commons.topic.config;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class MessageRetryTemplate extends RetryTemplate {

    public MessageRetryTemplate(final TopicConfig topicConfig) {
        Integer retriesCount = topicConfig.getRetriesCount();
        if (retriesCount == null || retriesCount.equals(-1)) {
            super.setRetryPolicy(new AlwaysRetryPolicy());
        } else {
            super.setRetryPolicy(new SimpleRetryPolicy(retriesCount));
        }

        Long backOffPeriod = topicConfig.getBackOffPeriod();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        if (backOffPeriod != null) {
            fixedBackOffPolicy.setBackOffPeriod(backOffPeriod);
        }
        super.setBackOffPolicy(fixedBackOffPolicy);
    }
}
