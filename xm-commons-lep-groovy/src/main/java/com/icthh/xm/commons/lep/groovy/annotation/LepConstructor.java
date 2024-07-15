package com.icthh.xm.commons.lep.groovy.annotation;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The LepConstructor annotation.
 * Generate constructor that get lepContext as input parameters and assign all final fields or fields marked with LepInject annotation
 * that present in lepContext.
 * Also, will be assigned fields that is "LepService". LepService - class annotated by LepConstructor annotation or
 * has constructor that get exactly one parameter lepContext and annotated by LepInjectableService annotation.
 * <br>
 * useLepFactory - if true, then LepService will be created using lepContext.lepServices.getInstance(ServiceClass)
 * if false, then LepService will be created using new ServiceClass(lepContext)
 * <br>
 * Note: if class has constructor with exactly one lepContext parameter, then this constructor will be used,
 * and generated code will be places before existing constructor code.
 */
@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass("com.icthh.xm.commons.lep.groovy.annotation.LepServiceTransformation")
public @interface LepConstructor {
    boolean useLepFactory() default true;
}
