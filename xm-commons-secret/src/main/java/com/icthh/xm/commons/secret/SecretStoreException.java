package com.icthh.xm.commons.secret;

/** The secret store is unreachable or failed. The message names the operation only, never a value. */
public class SecretStoreException extends RuntimeException {

    public SecretStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
