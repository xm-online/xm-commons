package com.icthh.xm.commons.exceptions;

import static java.util.Collections.singletonList;

import java.util.Collection;
import lombok.Getter;

public class SkipPermissionException extends RuntimeException {

    @Getter
    private final Collection<String> permissions;

    public SkipPermissionException(final String message, final Collection<String> permissions) {
        super(message);
        this.permissions = permissions;
    }

    public SkipPermissionException(final String message, final String permission) {
        super(message);
        this.permissions = singletonList(permission);
    }
}
