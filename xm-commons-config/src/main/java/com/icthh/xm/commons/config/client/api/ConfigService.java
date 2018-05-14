package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public interface ConfigService {

    Map<String, Configuration> getConfigurationMap();
    void onConfigurationChanged(Consumer<Configuration> listener);
    void updateConfigurations(Collection<Configuration> configurations);
}
