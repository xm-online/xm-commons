package com.icthh.xm.commons.client.feign;

import com.icthh.xm.commons.client.feign.config.OAuth2InterceptedFeignConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AliasFor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@FeignClient
public @interface AuthorizedFeignClient {

    @AliasFor(annotation = FeignClient.class, attribute = "name")
    String name() default "";

    @AliasFor(annotation = FeignClient.class, attribute = "configuration")
    Class<?>[] configuration() default OAuth2InterceptedFeignConfiguration.class;

    String url() default "";

    boolean decode404() default false;

    Class<?> fallback() default void.class;

    Class<?> fallbackFactory() default void.class;

    String path() default "";
}
