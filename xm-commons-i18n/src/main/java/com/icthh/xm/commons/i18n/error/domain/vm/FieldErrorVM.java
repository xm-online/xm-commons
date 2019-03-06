package com.icthh.xm.commons.i18n.error.domain.vm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * View Model for transferring error message with a list of field errors.
 */
@Getter
public class FieldErrorVM extends ErrorVM {

    private List<FieldError> fieldErrors;

    public FieldErrorVM(final String error) {
        super(error);
    }

    public FieldErrorVM(final String error, final String error_description) {
        super(error, error_description);
    }

    public void add(String objectName, String field, String message) {
        add(objectName, field, message, null);
    }

    public void add(String objectName, String field, String message, String description) {
        if (fieldErrors == null) {
            fieldErrors = new ArrayList<>();
        }
        fieldErrors.add(new FieldErrorVM.FieldError(objectName, field, message, description));
    }

    @Getter
    @RequiredArgsConstructor
    private static class FieldError implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String objectName;
        private final String field;
        private final String message;
        private final String description;

    }
}
