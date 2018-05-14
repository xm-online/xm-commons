package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.domain.Configuration;

import java.util.Collection;
import java.util.function.Consumer;

public interface ConfigurationModel {

    void onConfigurationChanged(Consumer<Configuration> listener);
    void updateConfiguration(Collection<Configuration> configurations);
}
