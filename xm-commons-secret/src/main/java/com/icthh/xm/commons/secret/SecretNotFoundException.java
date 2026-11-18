package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.exceptions.BusinessNotFoundException;

/** A secret or one of its fields does not exist. The message names the alias, never a value. */
public class SecretNotFoundException extends BusinessNotFoundException {

    public static final String CODE = "error.secret.not.found";

    SecretNotFoundException(String message) {
        super(CODE, message);
    }
}
