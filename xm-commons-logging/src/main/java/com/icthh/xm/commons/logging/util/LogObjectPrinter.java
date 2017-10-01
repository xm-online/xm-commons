package com.icthh.xm.commons.logging.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Utility class for object printing in Logging aspects.
 */
@Slf4j
public class LogObjectPrinter {

    private static final String XM_PACKAGE_NAME = "com.icthh.xm";
    private static final String PSWRD_MASK = "*****";
    private static final Set<String> MASK_SET = new HashSet<>();
    public static final String EMPTY_LIST_STRING = "[]";

    static {
        MASK_SET.add("newPassword");
        MASK_SET.add("password");
        MASK_SET.add("defaultPassword");
    }

    private LogObjectPrinter() {
    }

    public static String printException(Throwable e) {
        return String.valueOf(e);
    }

    public static String printExceptionWithStackInfo(Throwable e) {
        StringBuilder out = new StringBuilder();
        printExceptionWithStackInfo(e, out);
        return out.toString();
    }

    private static void printExceptionWithStackInfo(Throwable e, StringBuilder out) {
        out.append(e);
        if (e != null) {
            appendStackTrace(e, out);
            if (e.getCause() != null) {
                out.append(" -> ");
                printExceptionWithStackInfo(e.getCause(), out);
            }
        }
    }

    private static void appendStackTrace(Throwable e, StringBuilder out) {
        val stackTrace = e.getStackTrace();
        if (stackTrace == null || stackTrace.length < 1) {
            return;
        }

        out.append(" ").append(stackTrace[0]);

        for (int i = 1; i < stackTrace.length; i++) {
            String stackTraceLine = String.valueOf(stackTrace[i]);
            if (stackTrace[i].getClassName().startsWith(XM_PACKAGE_NAME) && !stackTraceLine.contains("<generated>")) {
                out.append(" ... ").append(stackTraceLine).append("...");
                break;
            }
        }
    }

    public static <T> String composeUrl(T[] arr, T... arr2) {
        try {
            T[] url = ArrayUtils.addAll(arr, arr2);
            String res = StringUtils.join(url);
            return res == null ? "" : res;
        } catch (Exception e) {
            log.warn("error while compose URL from: {}, {}", arr, arr2);
            return "printerror:" + e;
        }

    }

    public static String getCallMethod(JoinPoint joinPoint) {
        if (joinPoint != null && joinPoint.getSignature() != null) {
            Class<?> c = joinPoint.getSignature().getDeclaringType();
            String className = c != null ? c.getSimpleName() : "?";
            String methodName = joinPoint.getSignature().getName();
            return className + ":" + methodName;
        }
        return "?:?";
    }

    public static String printInputParams(JoinPoint joinPoint, String... paramNames) {
        try {

            if (joinPoint == null) {
                return "joinPoint is null";
            }

            Signature signature = joinPoint.getStaticPart().getSignature();
            if (!(signature instanceof MethodSignature)) {
                return EMPTY_LIST_STRING;
            }

            MethodSignature ms = (MethodSignature) signature;
            String[] params = ms.getParameterNames();
            if (ArrayUtils.isEmpty(params)) {
                return EMPTY_LIST_STRING;
            }

            return renderParams(joinPoint, params, paramNames);

        } catch (Exception e) {
            log.warn("error while print params: {}, params = {}", e, joinPoint.getArgs());
            return "printerror: " + e;
        }

    }

    private static String renderParams(JoinPoint joinPoint, String[] params, String[] paramNames) {
        Set<String> namesSet = prepareNameSet(paramNames);
        List<String> requestList = new ArrayList<>();

        Map<String, Object> map = joinPointToParamMap(joinPoint, params);

        if (!namesSet.isEmpty()) {
            namesSet.forEach(key -> requestList.add(buildParam(key, map.get(key))));
        } else {
            map.forEach((key, value) -> requestList.add(buildParam(key, value)));
        }

        return StringUtils.join(requestList, ',');
    }

    private static Map<String, Object> joinPointToParamMap(JoinPoint joinPoint, String[] params) {
        val map = new LinkedHashMap<String, Object>();
        IntStream.range(0, params.length).boxed().forEach(i -> map.put(params[i], joinPoint.getArgs()[i]));
        return map;
    }

    private static Set<String> prepareNameSet(String[] paramNames) {
        Set<String> namesSet = new LinkedHashSet<>();
        if (paramNames != null) {
            namesSet.addAll(Arrays.asList(paramNames));
        }
        return namesSet;
    }


    private static String buildParam(String name, Object value) {
        return name + "=" + (MASK_SET.contains(name) ? "*****" : value);
    }

    public static RestResp printRestResult(final Object res) {
        return printRestResult(res, true);
    }

    public static RestResp printRestResult(final Object res, final boolean printBody) {

        if (res == null) {
            return new RestResp("OK", "null", printBody);
        }

        Class<?> respClass = res.getClass();
        String status;
        Object bodyToPrint;

        if (ResponseEntity.class.isAssignableFrom(respClass)) {
            ResponseEntity<?> respEn = ResponseEntity.class.cast(res);

            status = String.valueOf(respEn.getStatusCode());

            Object body = respEn.getBody();
            bodyToPrint = printCollectionAware(body, printBody);

        } else {
            status = "OK";
            bodyToPrint = printCollectionAware(res, printBody);
        }
        return new RestResp(status, bodyToPrint, printBody);

    }

    @AllArgsConstructor
    @Getter
    public static class RestResp {
        private String status;
        private Object bodyToPrint;
        private boolean printBody;

        @Override
        public String toString() {
            return "status=" + status + (printBody ? ", body=" + bodyToPrint : "");
        }
    }

    public static Object printCollectionAware(final Object object) {
        return printCollectionAware(object, true);
    }

    public static Object printCollectionAware(final Object object, final boolean printBody) {
        if (object == null || !printBody) {
            return "";
        }
        Class<?> clazz = object.getClass();
        if (!Collection.class.isAssignableFrom(clazz)) {
            return object;
        }
        return new StringBuilder().append("[<")
                .append(clazz.getSimpleName())
                .append("> size = ")
                .append(Collection.class.cast(object).size()).append("]")
                .toString();

    }

}
