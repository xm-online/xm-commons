package com.icthh.xm.commons.config.client.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@UtilityClass
public class LockUtils {

    @SneakyThrows
    public static <R, E extends Exception> R runWithLock(Lock lock, long maxWaitTime, ReturnableTask<R, E> task) {
        log.info("Try to lock object");
        if (lock.tryLock(maxWaitTime, TimeUnit.SECONDS)) {
            log.info("Object locked");
            try {
                return task.execute();
            } finally {
                log.info("Try to unlock object");
                lock.unlock();
                log.info("Object unlocked");
            }
        } else {
            throw new IllegalMonitorStateException("Object already locked");
        }
    }

    public static <E extends Exception> void runWithLock(Lock lock, long maxWaitTime, Task<E> task) {
        runWithLock(lock, maxWaitTime, () -> {
            task.execute();
            return null;
        });
    }

}
