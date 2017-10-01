package com.icthh.xm.commons.errors.vm;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * View Model for sending a parametrized error message.
 */
@Getter
public class ParameterizedErrorVM extends ErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> params;

    public ParameterizedErrorVM(final String error) {
        this(error, null);
    }

    public ParameterizedErrorVM(final String error, String message) {
        this(error, message, new HashMap<>());
    }

    public ParameterizedErrorVM(String code, String message, Map<String, String> params) {
        super(code, message);
        this.params = params;
    }

}
