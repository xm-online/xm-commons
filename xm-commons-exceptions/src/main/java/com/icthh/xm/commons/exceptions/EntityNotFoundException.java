package com.icthh.xm.commons.exceptions;

/**
 * Custom esception for Not found entity by ID.
 * Created by medved on 21.06.17.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(final String message) {
        super(message);
    }

}
