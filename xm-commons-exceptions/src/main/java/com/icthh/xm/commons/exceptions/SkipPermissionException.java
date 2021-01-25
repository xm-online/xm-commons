package com.icthh.xm.commons.exceptions;

import lombok.Getter;

import java.util.Collection;

public class SkipPermissionException extends RuntimeException {

    @Getter
    private final Collection<String> permissions;

    public SkipPermissionException(final String message, final Collection<String> permissions) {
        super(message);
        this.permissions = permissions;
    }
}
