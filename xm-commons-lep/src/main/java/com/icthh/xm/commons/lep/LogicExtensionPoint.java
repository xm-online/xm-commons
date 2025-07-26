package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepKeyResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Language;

/**
 * LogicExtensionPoint annotation.
 */
@Target( {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicExtensionPoint {

    /**
     * Base LEP key name, or complete LEP key name if resolver not specified.
     *
     * @return logic extension point key name
     */
    String value();

    /**
     * LEP group name, it's just marker for grouping {@link LogicExtensionPoint}s (can be used
     * like Java package name convention).
     *
     * @return LEP group name
     */
    String group() default "";

    /**
     * LEP key resolver implementation class to determine complete (dynamic) LEP key.
     *
     * @return LEP key resolver implementation class
     */
    Class<? extends LepKeyResolver> resolver() default DefaultLepKeyResolver.class;

    /**
     * LEP key resolver expression to determine LEP key segments.
     * Use SPEL with method arguments in context to build expression that return string or list of strings.
     * <p>
     * If this attribute is specified, then {@link #resolver()} attribute is ignored.
     * </p>
     *
     * @return LEP key resolver expression
     */
    @Language("spel")
    String resolverExpression() default "";

}
