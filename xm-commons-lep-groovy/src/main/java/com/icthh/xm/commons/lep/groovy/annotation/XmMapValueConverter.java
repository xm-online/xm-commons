package com.icthh.xm.commons.lep.groovy.annotation;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;

/**
 * One step of the map-constructor value conversion chain. Converters are asked in a fixed order
 * via {@link #isSupport}; the first supporting converter builds the conversion expression.
 */
interface XmMapValueConverter {

    /** compile-time options of the current {@code @XmMapConstructor} usage */
    record ConversionOptions(boolean mapChildClasses, boolean mapCollections) {
    }

    boolean isSupport(ClassNode targetType, ConversionOptions options);

    Expression convert(ClassNode targetType, Expression value, ConversionOptions options);
}
