package com.icthh.xm.commons.config.client.repository;

/**
 * application.config-private-key is not set, the microservice can not read its configuration from xm-ms-config.
 * {@link com.icthh.xm.commons.config.client.config.ConfigPrivateKeyNotSetFailureAnalyzer} explains how to fix it.
 */
public class ConfigPrivateKeyNotSetException extends IllegalStateException {

    public ConfigPrivateKeyNotSetException() {
        super("application.config-private-key is not set: it is required to read the configuration "
            + "from xm-ms-config without the profile migration");
    }
}
