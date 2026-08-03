package com.icthh.xm.commons.lep.groovy.annotation;

import groovy.transform.Undefined;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a public {@code Map<String, Object>} constructor for a lep data class.
 * Extra map keys are ignored; every assignment is null-safe. Field conversion mirrors the
 * xm-ms-ee-config MapSupportGenerator: java.time types and UUID are parsed from String, enums are
 * mapped via the standard method without ever throwing, fields typed with another
 * {@code @XmMapConstructor} class are built via {@code new Child((Map) value)}, and List/Set/Map
 * fields of such classes are mapped per item. Leak-free replacement for groovy's
 * {@code @MapConstructor} (see GroovyMapConstructorTypeAnnotations).
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass("com.icthh.xm.commons.lep.groovy.annotation.XmMapConstructorTransformation")
public @interface XmMapConstructor {

    /** Closure inlined at the start of the generated constructor (after the super call). */
    Class pre() default Undefined.class;

    /** Closure inlined at the end of the generated constructor; sees the {@code map} parameter and assigned fields. */
    Class post() default Undefined.class;

    /** Field names the map constructor must not assign. */
    String[] excludes() default {};

    /** When false, fields typed with an {@code @XmMapConstructor} class get a plain cast instead of {@code new Child((Map) v)}. */
    boolean mapChildClasses() default true;

    /** When false, List/Set/Map fields get a plain cast instead of per-item mapping. */
    boolean mapCollections() default true;

    /** When false, no default (no-arg) constructor is generated alongside the map constructor. */
    boolean noArgConstructor() default true;
}
