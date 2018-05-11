package com.icthh.xm.commons.config.client.utils;

@FunctionalInterface
public interface Task<E extends Exception> {
    void execute() throws E;
}
