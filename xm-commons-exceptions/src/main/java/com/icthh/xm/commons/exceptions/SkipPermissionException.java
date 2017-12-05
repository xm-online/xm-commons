package com.icthh.xm.commons.exceptions;

public class SkipPermissionException extends RuntimeException {

    private String permission;

    public SkipPermissionException(final String message, final String permission) {
        super(message);
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

}
