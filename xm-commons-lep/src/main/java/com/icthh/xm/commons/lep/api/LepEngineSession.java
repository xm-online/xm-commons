package com.icthh.xm.commons.lep.api;

import java.io.Closeable;

@FunctionalInterface
public interface LepEngineSession extends Closeable {
    public void close();
}
