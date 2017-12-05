package com.icthh.xm.commons.exceptions;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom, parametrized exception, which can be translated on the client side.
 * For example:
 * <p>
 * <pre>
 * throw new BusinessException(&quot;myCustomError&quot;, &quot;hello&quot;, &quot;world&quot;);
 * </pre>
 * <p>
 * Can be translated with:
 * <p>
 * <pre>
 * "error.myCustomError" :  "The server says {{param0}} to {{param1}}"
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String PARAM = "param";

    private final String code;

    private final String message;

    private final Map<String, String> paramMap = new HashMap<>();

    public BusinessException(String message) {
        this(ErrorConstants.ERR_BUSINESS, message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String code, String message, Map<String, String> paramMap) {
        super(message);
        this.code = code;
        this.message = message;
        this.paramMap.putAll(paramMap);
    }

    public BusinessException(String message, Map<String, String> paramMap) {
        this(message);
        this.paramMap.putAll(paramMap);
    }

    public BusinessException withParams(String... params) {
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                paramMap.put(PARAM + i, params[i]);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "{code=" + code + ", message=" + message + (paramMap.isEmpty() ? "" : ", paramMap=" + paramMap) + "}";
    }
}
