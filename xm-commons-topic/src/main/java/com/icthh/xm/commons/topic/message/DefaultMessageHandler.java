package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "application.lep-message-handling.disabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DefaultMessageHandler implements MessageHandler {

    private final MessageService messageListenerService;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig, Map<String, byte[]> headers) {
        try {
            init(tenant);
            messageListenerService.onMessageWithHeaders(message, topicConfig, headers);
        } finally {
            destroy();
        }
    }

    private void init(String tenantKey) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
    }

    private void destroy() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }
}
