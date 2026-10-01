package com.icthh.xm.commons.config.client.config;

public interface LepContextRunner {
    void runInContext(Runnable task);
    boolean isReady();
}
