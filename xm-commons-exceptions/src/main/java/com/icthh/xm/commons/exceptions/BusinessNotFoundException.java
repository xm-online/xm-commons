package com.icthh.xm.commons.exceptions;

import java.util.Map;

/**
 * Extended exception from {@link BusinessException} with response code 404
 * @author maximbogun
 */
public class BusinessNotFoundException extends BusinessException {

    public BusinessNotFoundException(String message) {
        super(message);
    }

    public BusinessNotFoundException(String code, String message) {
        super(code, message);
    }

    public BusinessNotFoundException(String code, String message, Map<String, String> paramMap) {
        super(code, message, paramMap);
    }

    public BusinessNotFoundException(String message, Map<String, String> paramMap) {
        super(message, paramMap);
    }

}
