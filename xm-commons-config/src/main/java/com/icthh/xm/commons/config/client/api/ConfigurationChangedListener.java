package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;

@FunctionalInterface
public interface ConfigurationChangedListener {
    void onConfigurationChanged(Configuration configuration);
}
