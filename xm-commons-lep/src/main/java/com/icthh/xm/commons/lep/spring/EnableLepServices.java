package com.icthh.xm.commons.lep.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link EnableLepServices} annotation.
 * Scans for interfaces that declare they are LEP services (via {@link LepService
 * <code>@LepService</code>}). Configures component scanning directives for use with
 * {@link org.springframework.context.annotation.Configuration
 * <code>@Configuration</code>} classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(LepServicesRegistrar.class)
public @interface EnableLepServices {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @EnableLepServices("org.my.pkg")} instead of
     * {@code @EnableLepServices(basePackages="org.my.pkg")}.
     *
     * @return the array of 'basePackages'
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components.
     * <p>
     * {@link #value()} is an alias for (and mutually exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the array of 'basePackages'
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated components. The package of each class specified will be scanned.
     * <p>
     * Consider creating a special no-op marker class or interface in each package that
     * serves no purpose other than being referenced by this attribute.
     *
     * @return the array of 'basePackageClasses'
     */
    Class<?>[] basePackageClasses() default {};

}
