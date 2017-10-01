package com.icthh.xm.commons.timeline.utils;

import java.util.Base64;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

public class MDCUtil {

    private static final Integer RID_LENGTH = 8;

    private static final String RID = "rid";
    private static final String TIME = "time";

    private MDCUtil() {
    }

    public static void put() {
        put(RID);
    }

    public static void putRid(String rid) {
        put(RID, rid);
    }

    public static void remove() {
        remove(RID);
    }

    public static String getRid() {
        return MDC.get(RID);
    }

    public static long getTime() {
        return getTime(RID);
    }

    public static long getExecTime() {
        return getExecTime(RID);
    }

    public static void put(String key) {
        put(key, generateRid());
    }

    public static void put(String key, String rid) {
        MDC.put(key, rid == null ? generateRid() : rid);
        MDC.put(key + TIME, String.valueOf(System.nanoTime()));
    }

    public static void remove(String key) {
        MDC.remove(key);
        MDC.remove(key + TIME);
    }

    public static long getTime(String key) {
        String time = MDC.get(key + TIME);
        return StringUtils.isNotBlank(time) ? Long.valueOf(time) : 0;
    }

    public static long getExecTime(String key) {
        return (System.nanoTime() - getTime(key)) / 1000000;
    }

    public static void clear() {
        MDC.clear();
    }

    public static String generateRid() {
        String rid = new String(Base64.getEncoder().encode(DigestUtils.sha256(UUID.randomUUID().toString())));
        rid = StringUtils.replaceChars(rid, "+/=", "");
        return StringUtils.right(rid, RID_LENGTH);
    }

}