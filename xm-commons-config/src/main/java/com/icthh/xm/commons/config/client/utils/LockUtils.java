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
    public static <R, E extends Exception> R runWithLock(String objectName, Lock lock, long maxWaitTime, ReturnableTask<R, E> task) {
        log.info("Try to lock {}", objectName);
        if (lock.tryLock(maxWaitTime, TimeUnit.SECONDS)) {
            log.info("Object locked");
            try {
                return task.execute();
            } finally {
                log.info("Try to unlock {}", objectName);
                lock.unlock();
                log.info("{} unlocked", objectName);
            }
        } else {
            throw new IllegalMonitorStateException(objectName + " already locked");
        }
    }

    public static <E extends Exception> void runWithLock(String objectName, Lock lock, long maxWaitTime, Task<E> task) {
        runWithLock(objectName, lock, maxWaitTime, () -> {
            task.execute();
            return null;
        });
    }

}
