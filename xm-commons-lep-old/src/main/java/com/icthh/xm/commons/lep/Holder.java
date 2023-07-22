package com.icthh.xm.commons.lep;

/**
 * The {@link com.icthh.xm.commons.lep.Holder} class.
 */
public class Holder<T> {

    private static final Holder<?> EMPTY = new Holder<>(null);

    /**
     * The value contained in the holder.
     */
    private final T value;

    /**
     * Create a new holder with the specified value.
     *
     * @param value The value to be stored in the holder.
     */
    private Holder(T value) {
        this.value = value;
    }

    public T orElse(T other) {
        return (value != null) ? value : other;
    }

    public static <T> Holder<T> ofNullable(T value) {
        return (value == null) ? empty() : of(value);
    }

    public static <T> Holder<T> of(T value) {
        return new Holder<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Holder<T> empty() {
        return (Holder<T>) EMPTY;
    }

}
