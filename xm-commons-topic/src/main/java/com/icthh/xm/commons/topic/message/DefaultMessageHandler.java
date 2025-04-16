package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "lepManager")
@RequiredArgsConstructor
public class DefaultMessageHandler implements MessageHandler {

    private final MessageService messageListenerService;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig) {
        try {
            init(tenant);
            messageListenerService.onMessage(message, topicConfig);
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
