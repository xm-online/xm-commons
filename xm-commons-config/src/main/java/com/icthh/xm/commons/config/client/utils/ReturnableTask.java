package com.icthh.xm.commons.config.client.utils;

@FunctionalInterface
public interface ReturnableTask<R, E extends Exception> {
    R execute() throws E;
}
