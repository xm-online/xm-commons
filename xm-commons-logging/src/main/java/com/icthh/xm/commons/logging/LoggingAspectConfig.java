package com.icthh.xm.commons.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides possibility to configure logging aspect. Can be applied to Class or Method level.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LoggingAspectConfig {

    boolean DEFAULT_INPUT_DETAILS = true;
    boolean DEFAULT_INPUT_COLLECTION_AWARE = false;
    boolean DEFAULT_RESULT_DETAILS = true;
    boolean DEFAULT_RESULT_COLLECTION_AWARE = true;

    /**
     * If set prints method input parameters. Otherwise '#hidden#' string will be printed.
     *
     * Default: true
     */
    boolean inputDetails() default DEFAULT_INPUT_DETAILS;

    /**
     * In case input parameter object is a collection prints only size and type of it.
     *
     * Default: false
     */
    boolean inputCollectionAware() default DEFAULT_INPUT_COLLECTION_AWARE;

    /**
     * List of input parameter names to be included to print.
     *
     * Has higher priority if set both with {@code inputExcludeParams}.
     *
     * Default: {} - not set
     */
    String[] inputIncludeParams() default {};

    /**
     * List of input parameter names to be excluded from printing.
     *
     * Has lower priority if set both with {@code inputIncludeParams}
     *
     * Default: {} - not set
     */
    String[] inputExcludeParams() default {};

    /**
     * If set prints method result.
     *
     * Default: true
     */
    boolean resultDetails() default DEFAULT_RESULT_DETAILS;

    /**
     * In case result object is a collection prints only size and type of it.
     *
     * Default: true
     */
    boolean resultCollectionAware() default DEFAULT_RESULT_COLLECTION_AWARE;

}
