package com.icthh.xm.commons.config.client.repository;

public class ConfigPrivateKeyNotSetException extends IllegalStateException {

    public ConfigPrivateKeyNotSetException() {
        super("application.config-private-key is not set: it is required to read the configuration "
            + "from xm-ms-config without the profile migration");
    }
}
