package com.icthh.xm.commons.i18n.error.domain.vm;

import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

/**
 * View Model for sending a parametrized error message.
 */
@Getter
public class ParameterizedErrorVM extends ErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> params;

    public ParameterizedErrorVM(String error, String error_description, Map<String, String> params) {
        super(error, error_description);
        this.params = params;
    }

}
