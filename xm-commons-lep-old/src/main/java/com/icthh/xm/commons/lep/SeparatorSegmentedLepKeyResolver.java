package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link SeparatorSegmentedLepKeyResolver} class.
 */
public abstract class SeparatorSegmentedLepKeyResolver implements LepKeyResolver {

    /**
     * Average method parameters count for cache initial capacity.
     */
    private static final int AVERAGE_PARAMETERS_COUNT = 3;

    /**
     * Method parameter index cache by name.
     */
    private final Map<String, Integer> paramNameIndex = new ConcurrentHashMap<>(AVERAGE_PARAMETERS_COUNT);

    /**
     * Key template for parameter index cache.
     */
    private static final String INDEX_NAME_KEY_TEMPLATE = "%s:%s";

    /**
     * Translate from XmEntity naming convention to LEP script/key naming convention.
     * <br>
     * Replace symbols:<br>
     * {@code '-' -> '_' }<br>
     * {@code '.' -> '$' }
     *
     * @param xmEntitySpecKey input XmEntity key value
     * @return key value in LEP convention
     */
    protected static String translateToLepConvention(String xmEntitySpecKey) {
        Objects.requireNonNull(xmEntitySpecKey, "xmEntitySpecKey can't be null");
        return xmEntitySpecKey.replaceAll("-", "_").replaceAll("\\.", "\\$");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final LepKey resolve(LepKey baseKey, LepMethod method, LepManagerService managerService) {
        Objects.requireNonNull(baseKey, "baseKey can't be null");

        if (!(baseKey instanceof SeparatorSegmentedLepKey)) {
            throw new IllegalArgumentException("Unsupported base key type: " + baseKey.getClass().getCanonicalName());
        }

        SeparatorSegmentedLepKey segmentedLepKey = SeparatorSegmentedLepKey.class.cast(baseKey);

        return resolveKey(segmentedLepKey, method, managerService);
    }

    /**
     * Returns dynamic value for base LEP extension key.
     *
     * @param baseKey        not {@code null} base LEP extension key of type {@link SeparatorSegmentedLepKey}
     * @param method         LEP method
     * @param managerService manager service
     * @return dynamic value for base LEP extension key
     */
    protected abstract LepKey resolveKey(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService);

    protected Object getParamValue(LepMethod method, final String paramName) {
        int index = getParamIndex(method, paramName);
        return method.getMethodArgValues()[index];
    }

    protected <T> T getParamValue(LepMethod method, String paramName, Class<T> valueType) {
        Object value = getParamValue(method, paramName);

        return valueType.cast(value);
    }

    protected int getParamIndex(LepMethod method, final String paramName) {
        Objects.requireNonNull(paramName, "paramName can't be null");
        if (paramName.isEmpty()) {
            throw new IllegalArgumentException("paramName can't be blank");
        }
        String cacheKey = buildParamIndexCacheKey(method, paramName);
        Integer paramIndex = paramNameIndex.get(cacheKey);
        if (paramIndex == null) {
            String[] parameterNames = method.getMethodSignature().getParameterNames();
            for (int i = 0; i < parameterNames.length; i++) {
                if (paramName.equals(parameterNames[i])) {
                    paramIndex = i;
                    paramNameIndex.put(cacheKey, paramIndex);
                    break;
                }
            }
        }

        if (paramIndex == null) {
            throw new IllegalStateException("Can't find parameter '" + paramName + "' for method: "
                                                + method.getMethodSignature().toString());
        }

        return paramIndex;
    }

    private String buildParamIndexCacheKey(LepMethod method, final String paramName) {
        int cacheKey = method.getMethodSignature().getMethod().hashCode();
        return String.format(INDEX_NAME_KEY_TEMPLATE, cacheKey, paramName);
    }

    protected <T> T getRequiredParam(LepMethod method, String paramName, Class<T> valueType) {
        T value = getParamValue(method, paramName, valueType);
        if (value == null) {
            String methodDescription = getMethodDescription(method);

            throw new IllegalArgumentException("LEP method "
                                                   + methodDescription
                                                   + " required in parameter '" + paramName + "' is null");
        }
        return value;
    }

    private String getMethodDescription(LepMethod method) {
        MethodSignature methodSignature = method.getMethodSignature();
        if (methodSignature != null && methodSignature.getMethod() != null) {
            return methodSignature.getMethod().toString();
        }
        return method.toString();
    }

    protected String getRequiredStrParam(LepMethod method, String paramName) {
        return getRequiredParam(method, paramName, String.class);
    }

    protected String getStrParam(LepMethod method, String paramName) {
        return getParamValue(method, paramName, String.class);
    }

}
