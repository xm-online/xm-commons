package com.icthh.xm.commons.lep.groovy.annotation;

import groovy.transform.Undefined;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field-level custom conversion for {@code @XmMapConstructor} / {@code @XmToMap} generated code.
 * A field carrying a closure here bypasses the standard type dispatch for that direction; the
 * closure receives the value as {@code it} and is only called for non-null values.
 */
@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface XmMapConvert {

    /** closure converting the raw map value into the field value in the generated map constructor */
    Class from() default Undefined.class;

    /** closure converting the field value into the serialized value in the generated {@code toMap()} */
    Class to() default Undefined.class;
}
