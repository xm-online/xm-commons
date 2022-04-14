package com.icthh.xm.commons.config.client.api;

import java.util.Collection;

public interface RefreshableConfiguration {

    void onRefresh(String updatedKey, String config);

    boolean isListeningConfiguration(String updatedKey);

    default void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

    default void refreshFinished(Collection<String> paths) {
        //
    }
}
