package com.icthh.xm.commons.lep.impl;

@FunctionalInterface
public interface LepExecutionCallback {
    Object execute() throws Throwable;
}
