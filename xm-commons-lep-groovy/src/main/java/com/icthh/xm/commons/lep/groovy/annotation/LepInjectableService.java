package com.icthh.xm.commons.lep.groovy.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The LepInjectableService annotation.
 * Mark class as LepService. Must have no-args constructor or constructor with exactly one parameter lepContext
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LepInjectableService {
}
