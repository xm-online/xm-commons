package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;

import java.util.Collection;
import java.util.Map;

public interface ConfigService {

    Map<String, Configuration> getConfigurationMap(String commit);
    Map<String, Configuration> getConfigurationMap(String commit, Collection<String> paths);
    Map<String, Configuration> getConfigMapAntPattern(String commit, Collection<String> patternPaths);
    void addConfigurationChangedListener(ConfigurationChangedListener listener);
    void updateConfigurations(String commit, Collection<String> paths);
}
