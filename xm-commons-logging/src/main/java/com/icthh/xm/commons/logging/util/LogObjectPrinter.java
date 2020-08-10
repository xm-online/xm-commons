package com.icthh.xm.commons.logging.util;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import java.lang.reflect.Array;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Utility class for object printing in Logging aspects.
 */
@Slf4j
public final class LogObjectPrinter {

    private static final String XM_PACKAGE_NAME = "com.icthh.xm";
    private static final String PASSWORD_MASK = "*****";
    private static final Set<String> MASK_SET = new HashSet<>();
    private static final String[] EMPTY_ARRAY = new String[0];
    private static final String PRINT_EMPTY_LIST = "[]";
    private static final String PRINT_HIDDEN = "#hidden#";
    private static final String PRINT_QUESTION = "?";
    private static final String PRINT_SEMICOLON = ":";
    private static final String PRINT_EMPTY_METHOD = PRINT_QUESTION + PRINT_SEMICOLON + PRINT_QUESTION;


    static {
        MASK_SET.add("newPassword");
        MASK_SET.add("password");
        MASK_SET.add("defaultPassword");
        MASK_SET.add("secret");
    }

    private LogObjectPrinter() {
    }

    /**
     * Builds log string for exception.
     *
     * @param throwable the exception
     * @return exception description string
     */
    public static String printException(Throwable throwable) {
        return String.valueOf(throwable);
    }

    /**
     * Builds log string for exception with stack trace.
     *
     * @param throwable the exception
     * @return exception description string with stack trace
     */
    public static String printExceptionWithStackInfo(Throwable throwable) {
        StringBuilder out = new StringBuilder();
        printExceptionWithStackInfo(throwable, out);
        return out.toString();
    }

    private static void printExceptionWithStackInfo(Throwable throwable, StringBuilder out) {
        out.append(throwable);
        if (throwable != null) {
            appendStackTrace(throwable, out);
            if (throwable.getCause() != null) {
                out.append(" -> ");
                printExceptionWithStackInfo(throwable.getCause(), out);
            }
        }
    }

    private static void appendStackTrace(Throwable throwable, StringBuilder out) {
        val stackTrace = throwable.getStackTrace();
        if (stackTrace == null || stackTrace.length < 1) {
            return;
        }

        out.append(' ').append(stackTrace[0]);

        for (int i = 1; i < stackTrace.length; i++) {
            String stackTraceLine = String.valueOf(stackTrace[i]);
            if (stackTrace[i].getClassName().startsWith(XM_PACKAGE_NAME) && !stackTraceLine.contains("<generated>")) {
                out.append(" ... ").append(stackTraceLine).append("...");
                break;
            }
        }
    }

    /**
     * Join URL path into one string.
     *
     * @param arr  first url paths
     * @param arr2 other url paths
     * @param <T>  url part path type
     * @return URL representation string
     */
    @SafeVarargs
    public static <T> String joinUrlPaths(final T[] arr, final T... arr2) {
        try {
            T[] url = ArrayUtils.addAll(arr, arr2);
            String res = StringUtils.join(url);
            return (res == null) ? "" : res;
        } catch (IndexOutOfBoundsException | IllegalArgumentException | ArrayStoreException e) {
            log.warn("Error while join URL paths from: {}, {}", arr, arr2);
            return "printerror:" + e;
        }
    }

    /**
     * Gets method description string from join point.
     *
     * @param joinPoint aspect join point
     * @return method description string from join point
     */
    public static String getCallMethod(JoinPoint joinPoint) {
        if (joinPoint != null && joinPoint.getSignature() != null) {
            Class<?> declaringType = joinPoint.getSignature().getDeclaringType();
            String className = (declaringType != null) ? declaringType.getSimpleName() : PRINT_QUESTION;
            String methodName = joinPoint.getSignature().getName();
            return className + PRINT_SEMICOLON + methodName;
        }
        return PRINT_EMPTY_METHOD;
    }

    /**
     * Gets join point input params description string.
     *
     * @param joinPoint         aspect join point
     * @param includeParamNames input parameters names to be printed. NOTE! can be overridden with @{@link
     *                          LoggingAspectConfig}
     * @return join point input params description string
     */
    public static String printInputParams(JoinPoint joinPoint, String... includeParamNames) {
        try {
            if (joinPoint == null) {
                return "joinPoint is null";
            }

            Signature signature = joinPoint.getSignature();
            if (!(signature instanceof MethodSignature)) {
                return PRINT_EMPTY_LIST;
            }

            Optional<LoggingAspectConfig> config = AopAnnotationUtils.getConfigAnnotation(joinPoint);

            String[] includeParams = includeParamNames;
            String[] excludeParams = EMPTY_ARRAY;
            boolean inputCollectionAware = LoggingAspectConfig.DEFAULT_INPUT_COLLECTION_AWARE;

            if (config.isPresent()) {
                if (!config.get().inputDetails()) {
                    return PRINT_HIDDEN;
                }
                inputCollectionAware = config.get().inputCollectionAware();
                if (ArrayUtils.isNotEmpty(config.get().inputIncludeParams())) {
                    includeParams = config.get().inputIncludeParams();
                }
                if (ArrayUtils.isEmpty(includeParams) && ArrayUtils.isNotEmpty(config.get().inputExcludeParams())) {
                    excludeParams = config.get().inputExcludeParams();
                }
            }

            MethodSignature ms = (MethodSignature) signature;
            String[] params = ms.getParameterNames();
            return ArrayUtils.isNotEmpty(params) ? renderParams(joinPoint,
                                                                params,
                                                                includeParams,
                                                                excludeParams,
                                                                inputCollectionAware) : PRINT_EMPTY_LIST;
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            log.warn("Error while print params: {}, params = {}", e, joinPoint.getArgs());
            return "printerror: " + e;
        }
    }

    private static String renderParams(JoinPoint joinPoint, String[] params, String[] includeParamNames,
                                       String[] excludeParamNames, boolean inputCollectionAware) {

        Set<String> includeSet = prepareNameSet(includeParamNames);
        Set<String> excludeSet = prepareNameSet(excludeParamNames);
        List<String> requestList = new ArrayList<>();

        Map<String, Object> paramMap = joinPointToParamMap(joinPoint, params);

        if (!includeSet.isEmpty()) {
            includeSet
                .stream().filter(paramMap::containsKey)
                .forEach(key -> requestList.add(buildParam(key, paramMap.get(key), inputCollectionAware)));
        } else if (!excludeSet.isEmpty()) {
            paramMap.forEach((key, value) -> {
                if (!excludeSet.contains(key)) {
                    requestList.add(buildParam(key, value, inputCollectionAware));
                }
            });
        } else {
            paramMap.forEach((key, value) -> requestList.add(buildParam(key, value, inputCollectionAware)));
        }

        return StringUtils.join(requestList, ',');
    }

    private static Map<String, Object> joinPointToParamMap(JoinPoint joinPoint, String[] params) {
        val map = new LinkedHashMap<String, Object>();
        IntStream.range(0, params.length).boxed().forEach(index -> map.put(params[index], joinPoint.getArgs()[index]));
        return map;
    }

    private static Set<String> prepareNameSet(String[] paramNames) {
        return !ArrayUtils.isEmpty(paramNames) ? new LinkedHashSet<>(Arrays.asList(paramNames)) : Collections
            .emptySet();
    }

    private static String buildParam(String name, Object value, boolean inputCollectionAware) {

        StringBuilder builder = new StringBuilder(name).append("=");
        if (MASK_SET.contains(name)) {
            builder.append(PASSWORD_MASK);
        } else if (inputCollectionAware) {
            builder.append(printCollectionAware(value));
        } else {
            builder.append(printTypeAware(value));
        }
        return builder.toString();
    }

    /**
     * Print Result object according to input parameters and {@link LoggingAspectConfig}.
     * @param joinPoint         - intercepting join point
     * @param object            - result value to be printed
     */
    public static String printResult(final JoinPoint joinPoint, final Object object) {
        return printResult(joinPoint, object, LoggingAspectConfig.DEFAULT_RESULT_DETAILS);
    }

    /**
     * Print Result object according to input parameters and {@link LoggingAspectConfig}.
     *
     * @param joinPoint         - intercepting join point
     * @param object            - result value to be printed
     * @param printResultDetail - print result detail flag for programming approach. NOTE! can me overridden with {@link
     *                          LoggingAspectConfig}
     */
    public static String printResult(final JoinPoint joinPoint, final Object object, final boolean printResultDetail) {

        Optional<LoggingAspectConfig> config = AopAnnotationUtils.getConfigAnnotation(joinPoint);

        boolean resultDetails = printResultDetail;
        boolean resultCollectionAware = LoggingAspectConfig.DEFAULT_RESULT_COLLECTION_AWARE;

        if (config.isPresent()) {
            resultDetails = config.get().resultDetails();
            resultCollectionAware = config.get().resultCollectionAware();
        }

        if (!resultDetails) {
            return PRINT_HIDDEN;
        }

        if (resultCollectionAware) {
            return printCollectionAware(object);
        }

        return String.valueOf(object);

    }

    /**
     * Gets object representation with size for collection case.
     *
     * @param object object instance to log
     * @return object representation with size for collection case
     */
    public static String printCollectionAware(final Object object) {
        return printCollectionAware(object, true);
    }

    /**
     * Gets object representation with size for collection case.
     *
     * @param object    object instance to log
     * @param printBody if {@code true} then prevent object string representation
     * @return object representation with size for collection case
     */
    public static String printCollectionAware(final Object object, final boolean printBody) {

        if (!printBody) {
            return PRINT_HIDDEN;
        }

        if (object == null) {
            return "null";
        }

        Class<?> clazz = object.getClass();
        if (Collection.class.isAssignableFrom(clazz)) {
            return "[<"
                   + clazz.getSimpleName()
                   + "> size = "
                   + ((Collection) object).size() + "]";
        } else if (clazz.isArray()) {
            return "[<"
                   + clazz.getSimpleName()
                   + "> length = "
                   + Array.getLength(object) + "]";
        }

        return object.toString();

    }

    private static String printTypeAware(Object value) {
        if (value == null) {
            return null;
        }
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }

}
