package com.icthh.xm.commons.i18n.error.domain.vm;

import lombok.Getter;

import java.io.Serializable;
import org.slf4j.MDC;

/**
 * The root view model for all error responses from rest API.
 * Created by medved on 20.06.17.
 */
@Getter
public class ErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final String error;
    protected final String error_description;
    protected final String requestId = MDC.get("rid");

    public ErrorVM(String error) {
        this(error, null);
    }

    public ErrorVM(String error, String error_description) {
        this.error = error;
        this.error_description = error_description;
    }

}
