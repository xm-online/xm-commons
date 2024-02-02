package com.icthh.xm.commons.topic.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.adapter.AbstractRetryingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.lang.Nullable;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryState;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;

// todo spring 3.2.0 migration (tmp copy from previous spring)
public class RetryingMessageListenerAdapter <K, V>
    extends AbstractRetryingMessageListenerAdapter<K, V, MessageListener<K, V>>
    implements AcknowledgingConsumerAwareMessageListener<K, V> {

    public static final String CONTEXT_ACKNOWLEDGMENT = "acknowledgment";
    public static final String CONTEXT_CONSUMER = "consumer";
    public static final String CONTEXT_RECORD = "record";

    private boolean stateful;

    public RetryingMessageListenerAdapter(MessageListener<K, V> delegate, RetryTemplate retryTemplate, RecoveryCallback<?> recoveryCallback, boolean stateful) {
        super(delegate, retryTemplate, recoveryCallback);
        this.stateful = stateful;
    }

    @Override
    public void onMessage(final ConsumerRecord<K, V> record, @Nullable final Acknowledgment acknowledgment,
                          final Consumer<?, ?> consumer) {

        RetryState retryState = null;
        if (this.stateful) {
            retryState = new DefaultRetryState(record.topic() + "-" + record.partition() + "-" + record.offset());
        }
        getRetryTemplate().execute(context -> {
                context.setAttribute(CONTEXT_RECORD, record);
                switch (RetryingMessageListenerAdapter.this.delegateType) {
                    case ACKNOWLEDGING_CONSUMER_AWARE:
                        context.setAttribute(CONTEXT_ACKNOWLEDGMENT, acknowledgment);
                        context.setAttribute(CONTEXT_CONSUMER, consumer);
                        RetryingMessageListenerAdapter.this.delegate.onMessage(record, acknowledgment, consumer);
                        break;
                    case ACKNOWLEDGING:
                        context.setAttribute(CONTEXT_ACKNOWLEDGMENT, acknowledgment);
                        RetryingMessageListenerAdapter.this.delegate.onMessage(record, acknowledgment);
                        break;
                    case CONSUMER_AWARE:
                        context.setAttribute(CONTEXT_CONSUMER, consumer);
                        RetryingMessageListenerAdapter.this.delegate.onMessage(record, consumer);
                        break;
                    case SIMPLE:
                        RetryingMessageListenerAdapter.this.delegate.onMessage(record);
                }
                return null;
            },
            getRecoveryCallback(), retryState);
    }

    /*
     * Since the container uses the delegate's type to determine which method to call, we
     * must implement them all.
     */

    @Override
    public void onMessage(ConsumerRecord<K, V> data) {
        onMessage(data, null, null); // NOSONAR
    }

    @Override
    public void onMessage(ConsumerRecord<K, V> data, Acknowledgment acknowledgment) {
        onMessage(data, acknowledgment, null); // NOSONAR
    }

    @Override
    public void onMessage(ConsumerRecord<K, V> data, Consumer<?, ?> consumer) {
        onMessage(data, null, consumer);
    }

}
