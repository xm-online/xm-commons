package com.icthh.xm.commons.logging.util;

import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * Utility class for MDC processing.
 */
public final class MdcUtils {

    private static final int RID_LENGTH = 8;
    private static final String RID = "rid";
    private static final String TIME = "time";
    private static final long LONG_ZERO = 0L;

    private MdcUtils() {
        throw new IllegalAccessError("Access not allowed");
    }

    public static void putRid() {
        put(RID);
    }

    public static void putRid(String ridValue) {
        put(RID, ridValue);
    }

    public static void put(String key) {
        put(key, generateRid());
    }

    public static void put(String key, String value) {
        MDC.put(key, (value == null) ? generateRid() : value);
        MDC.put(key + TIME, String.valueOf(System.nanoTime()));
    }

    public static String getRid() {
        return MDC.get(RID);
    }

    public static void removeRid() {
        remove(RID);
    }

    public static void remove(String key) {
        MDC.remove(key);
        MDC.remove(key + TIME);
    }

    public static long getRidTimeNs() {
        return getTimeNs(RID);
    }

    public static long getTimeNs(String key) {
        String time = MDC.get(key + TIME);
        return StringUtils.isNotBlank(time) ? Long.parseLong(time) : LONG_ZERO;
    }

    public static long getExecTimeMs() {
        return getExecTimeMs(RID);
    }

    public static long getExecTimeMs(String key) {
        long timeNs = getTimeNs(key);
        return timeNs == LONG_ZERO ? LONG_ZERO : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timeNs);
    }

    public static void clear() {
        MDC.clear();
    }

    /**
     * Generates request id based on UID and SHA-256.
     *
     * @return request identity
     */
    public static String generateRid() {
        byte[] bytes = new byte[RID_LENGTH];
        ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
            .substring(0, RID_LENGTH);
    }

}

