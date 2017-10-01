package com.icthh.xm.commons.config.client.api;

public interface RefreshableConfiguration {

    void onRefresh(String updatedKey, String config);

    boolean isListeningConfiguration(String updatedKey);

    void onInit(String configKey, String configValue);

}
