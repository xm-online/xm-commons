package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface ConfigurationChangedListener {
    void onConfigurationChanged(Configuration configuration);

    default void refreshFinished(Collection<String> paths) {
        // NO NOTHING
    }
}
