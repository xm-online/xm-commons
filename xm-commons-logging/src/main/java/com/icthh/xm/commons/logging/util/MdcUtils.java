package com.icthh.xm.commons.logging.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for MDC processing.
 */
public final class MdcUtils {

    private static final int RID_LENGTH = 8;
    private static final String RID = "rid";
    private static final String TIME = "time";

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
        return StringUtils.isNotBlank(time) ? Long.parseLong(time) : 0L;
    }

    public static long getExecTimeMs() {
        return getExecTimeMs(RID);
    }

    public static long getExecTimeMs(String key) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - getTimeNs(key));
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
        byte[] encode = Base64.getEncoder().encode(DigestUtils.sha256(UUID.randomUUID().toString()));
        try {
            String rid = new String(encode, StandardCharsets.UTF_8.name());
            rid = StringUtils.replaceChars(rid, "+/=", "");
            return StringUtils.right(rid, RID_LENGTH);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

}

