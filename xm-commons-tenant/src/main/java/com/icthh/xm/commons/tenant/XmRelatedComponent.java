package com.icthh.xm.commons.tenant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// marker o filter xm components in all spring beans
@Retention(RetentionPolicy.RUNTIME)
public @interface XmRelatedComponent {
    // context label
    String value() default "";
}
