package com.icthh.xm.commons.lep.spring;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LepService annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface LepService {

    /**
     * Default group name.
     */
    String DEFAULT_GROUP = "general";

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     * @return the suggested component name, if any
     */
    @AliasFor("name")
    String value() default "";

    /**
     * The service id with optional protocol prefix. Synonym for {@link #value() value}.
     */
    @AliasFor("value")
    String name() default "";

    /**
     * LEP group name for all methods in target type, it's just marker for grouping LEPs (can be used
     * like Java package name convention).
     *
     * @return LEP group name
     */
    String group() default DEFAULT_GROUP;

    /**
     * If true only methods annotated with @LogicExtensionPoint will be LEP.
     * <p>
     * If false every method except constructors and Object class methods will be LEP.
     *
     * @return true for explicitly LEP methods annotation
     */
    boolean explicitMethods() default true;

    /**
     * Sets the {@code @Qualifier} value for the Lep service.
     */
    String qualifier() default "";

    /**
     * Whether to mark the LepService as a primary bean. Defaults to false.
     */
    boolean primary() default false;

}
