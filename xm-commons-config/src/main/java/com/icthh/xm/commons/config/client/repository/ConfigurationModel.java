package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.domain.Configuration;

public interface ConfigurationModel {

    void setConfigurationListener(ConfigurationListener configurationListener);
    void updateConfiguration(Configuration configuration);
}
