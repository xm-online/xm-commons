package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LepMessageHandler implements MessageHandler {

    private final MessageService messageListenerService;
    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig) {
        tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenant), () -> {
            try (var context = lepManagementService.beginThreadContext()) {
                messageListenerService.onMessage(message, topicConfig);
            }
        });
    }
}
