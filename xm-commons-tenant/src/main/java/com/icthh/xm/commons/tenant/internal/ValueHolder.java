package com.icthh.xm.commons.tenant.internal;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The {@link ValueHolder} class.
 */
final class ValueHolder<T> {

    private static volatile ValueHolder<?> emptyInstance = new ValueHolder<>();

    private final T value;

    private ValueHolder() {
        this.value = null; //NOPMD
    }

    private ValueHolder(T value) {
        this.value = Objects.requireNonNull(value);
    }

    @SuppressWarnings("unchecked")
    static <T> ValueHolder<T> empty() {
        return (ValueHolder<T>) emptyInstance;
    }

    static <T> ValueHolder<T> valueOf(T value) {
        return new ValueHolder<>(value);
    }

    boolean isPresent() {
        return value != null;
    }

    boolean isEmpty() {
        return this == emptyInstance;
    }

    T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

}
