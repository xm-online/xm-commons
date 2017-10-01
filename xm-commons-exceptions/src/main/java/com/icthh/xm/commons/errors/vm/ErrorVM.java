package com.icthh.xm.commons.errors.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

/**
 * The root view model for all error responses from rest API.
 * Created by medved on 20.06.17.
 */
@Getter
public class ErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String error;
    @JsonProperty("error_description")
    protected final String errorDescription;

    public ErrorVM(String error) {
        this(error, null);
    }

    public ErrorVM(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

}
