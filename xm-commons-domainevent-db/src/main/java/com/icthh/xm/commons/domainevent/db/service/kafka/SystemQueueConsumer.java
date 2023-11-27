package com.icthh.xm.commons.domainevent.db.service.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

@Slf4j
@Service
public class SystemQueueConsumer {

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;
    private final SystemConsumerService systemConsumerService;
    private final LepManager lepManager;

    public SystemQueueConsumer(TenantContextHolder tenantContextHolder,
                               XmAuthenticationContextHolder authContextHolder,
                               SystemConsumerService systemConsumerService,
                               LepManager lepManager) {
        this.tenantContextHolder = tenantContextHolder;
        this.authContextHolder = authContextHolder;
        this.systemConsumerService = systemConsumerService;
        this.lepManager = lepManager;
    }

    /**
     * Consume system event message.
     *
     * @param message the system event message
     */
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MdcUtils.putRid();
        try {
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
        } finally {
            destroy();
        }
    }

    private void init(String tenantKey, String login) {
        if (StringUtils.isNotBlank(tenantKey)) {
            TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

            lepManager.beginThreadContext(threadContext -> {
                threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
                threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
            });
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
