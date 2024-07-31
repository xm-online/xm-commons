package com.icthh.xm.commons.lep.groovy.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The LepInject annotation.
 * Work only with LepConstructor annotation.
 * Mark field that should be injected from LepContext on constructor.
 */
@Target({ElementType.FIELD})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface LepInject {
}
