package com.icthh.xm.commons.domainevent.db.service.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.logging.trace.SleuthWrapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SystemQueueConsumer {

    private final TenantContextHolder tenantContextHolder;
    private final SystemConsumerService systemConsumerService;
    private final LepManagementService lepManager;
    private final SleuthWrapper sleuthWrapper;

    public SystemQueueConsumer(TenantContextHolder tenantContextHolder,
        SystemConsumerService systemConsumerService,
        LepManagementService lepManager, SleuthWrapper sleuthWrapper) {
        this.tenantContextHolder = tenantContextHolder;
        this.systemConsumerService = systemConsumerService;
        this.lepManager = lepManager;
        this.sleuthWrapper = sleuthWrapper;
    }

    /**
     * Consume system event message.
     *
     * @param message the system event message
     */
    @LoggingAspectConfig(inputDetails = false)
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MdcUtils.putRid();
        try {
            sleuthWrapper.runWithSleuth(message, () -> processEvent(message));
        } finally {
            destroy();
        }
    }

    private void processEvent(ConsumerRecord<String, String> message) {
        log.info("Consume event from topic [{}]", message.topic());
        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        try {
            SystemEvent event = mapper.readValue(message.value(), SystemEvent.class);

            log.info("Process event from topic [{}], {}", message.topic(), event);

            if (StringUtils.isBlank(event.getTenantKey())) {
                log.info("Event ignored due to tenantKey is empty {}", event);
                return;
            }
            init(event.getTenantKey(), event.getUserLogin());

            systemConsumerService.acceptSystemEvent(event);
        } catch (IOException e) {
            log.error("System queue message has incorrect format: '{}'", message.value(), e);
        }
    }

    private void init(String tenantKey, String login) {
        if (StringUtils.isNotBlank(tenantKey)) {
            TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
            lepManager.beginThreadContext();
        }

        String newRid = MdcUtils.getRid()
            + ":" + StringUtils.defaultIfBlank(login, "")
            + ":" + StringUtils.defaultIfBlank(tenantKey, "");
        MdcUtils.putRid(newRid);
    }

    private void destroy() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        MdcUtils.removeRid();
    }
}
