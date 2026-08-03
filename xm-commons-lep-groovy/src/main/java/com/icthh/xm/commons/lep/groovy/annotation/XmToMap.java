package com.icthh.xm.commons.lep.groovy.annotation;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a public {@code Map<String, Object> toMap()} method: LinkedHashMap result,
 * {@code super.toMap()} merged first when the superclass is annotated, java.time/UUID as
 * {@code toString()}, nested {@code @XmToMap} fields as {@code toMap()}, collections serialized
 * per item, enums as {@code value()}/{@code name()}. Mirrors MapSupportGenerator#addToMapMethod.
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass("com.icthh.xm.commons.lep.groovy.annotation.XmToMapTransformation")
public @interface XmToMap {
}
