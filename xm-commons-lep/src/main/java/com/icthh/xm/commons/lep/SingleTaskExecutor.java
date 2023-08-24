package com.icthh.xm.commons.lep;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingleTaskExecutor extends ThreadPoolExecutor {
    public SingleTaskExecutor() {
        super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        // Clear the queue
        getQueue().clear();

        // Submit the new task
        return super.submit(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
        // Clear the queue
        getQueue().clear();

        // Submit the new task
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        // Clear the queue
        getQueue().clear();

        // Submit the new task
        return super.submit(task, result);
    }
}




