package com.icthh.xm.commons.lep.api;

public interface LepExecutorResolver {

    LepExecutor getLepExecutor(LepKey lepKey);

    void acquireUsage();

    void releaseUsage();
}
