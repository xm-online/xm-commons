package com.icthh.xm.commons.domain.event.config;

import com.icthh.xm.commons.domain.event.service.Transport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class SourceConfig {

    private final static String TRANSPORT_PACKAGE = Transport.class.getPackageName();
    private boolean enabled;
    private Class<? extends Transport> transport;

    public void setTransport(String transport) throws ClassNotFoundException {
        this.transport = getTransportClass(transport);
    }

    private Class<? extends Transport> getTransportClass(String name) throws ClassNotFoundException {
        Class<?> transport = Class.forName(TRANSPORT_PACKAGE + ".impl." + name);
        if (Transport.class.isAssignableFrom(transport)) {
            return (Class<? extends Transport>) transport;
        } else {
            throw new IllegalStateException(String.format(
                "Class %s, does not implement %s class.", transport.getName(), Transport.class.getName()
            ));
        }
    }
}
