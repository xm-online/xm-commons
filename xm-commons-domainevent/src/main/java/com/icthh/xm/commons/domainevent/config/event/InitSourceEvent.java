package com.icthh.xm.commons.domainevent.config.event;

import org.springframework.context.ApplicationEvent;

public class InitSourceEvent extends ApplicationEvent {

    private String tenantKey;
    private String transport;

    public InitSourceEvent(Object source, String tenantKey, String transport) {
        super(source);
        this.tenantKey = tenantKey;
        this.transport = transport;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public String getTransport() {
        return transport;
    }
}
