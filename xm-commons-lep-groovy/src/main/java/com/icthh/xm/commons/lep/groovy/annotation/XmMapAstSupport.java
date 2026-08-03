package com.icthh.xm.commons.lep.groovy.annotation;

import com.icthh.xm.commons.lep.groovy.XmMapSupport;
import com.icthh.xm.commons.lep.groovy.annotation.XmMapValueConverter.ConversionOptions;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;

import java.util.List;
import java.util.Set;

import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;

/**
 * Compile-time type dispatch shared by XmMapConstructorTransformation and XmToMapTransformation.
 * Never returns a shared ClassNode: every emitted node is a fresh plain reference.
 */
final class XmMapAstSupport {

    private static final Set<String> PARSEABLE_JAVA_TIME = Set.of(
        "java.time.Instant", "java.time.LocalDate", "java.time.LocalTime",
        "java.time.LocalDateTime", "java.time.OffsetDateTime", "java.time.OffsetTime",
        "java.time.ZonedDateTime", "java.time.Duration", "java.time.Period",
        "java.time.Year", "java.time.YearMonth", "java.time.MonthDay");

    /** conversion chain: the first supporting converter wins; the default cast accepts everything */
    private static final List<XmMapValueConverter> CONVERTERS = List.of(
        new JavaTimeConverter(),
        new UuidConverter(),
        new EnumConverter(),
        new ChildClassConverter(),
        new CollectionConverter(),
        new MapFieldConverter(),
        new DefaultCastConverter());

    private XmMapAstSupport() {
    }

    static Expression convertValue(ClassNode targetType, Expression value, ConversionOptions options) {
        for (XmMapValueConverter converter : CONVERTERS) {
            if (converter.isSupport(targetType, options)) {
                return converter.convert(targetType, value, options);
            }
        }
        throw new IllegalStateException("No converter supports " + targetType.getName());
    }

    private static class JavaTimeConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            return isParseableJavaTime(targetType);
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            return staticCall(targetType, "parse", castToString(value));
        }
    }

    private static class UuidConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            return isUuid(targetType);
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            return staticCall(targetType, "fromString", castToString(value));
        }
    }

    /** never throws at runtime: unknown enum values become null, remappable in the post closure */
    private static class EnumConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            return targetType.isEnum();
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            boolean hasFromValue = !targetType.getMethods("fromValue").isEmpty();
            Expression rawEnum = hasFromValue
                ? callX(new ClassExpression(plain(XmMapSupport.class)), "safeEnum",
                    args(value, closureOfIt(staticCall(targetType, "fromValue", castToString(varX("it"))))))
                : callX(new ClassExpression(plain(XmMapSupport.class)), "toEnum",
                    args(new ClassExpression(plain(targetType)), value));
            return castX(plain(targetType), rawEnum);
        }
    }

    private static class ChildClassConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            return options.mapChildClasses() && isXmMapConstructor(targetType);
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            return ctorX(plain(targetType), args(castX(plain(java.util.Map.class), value)));
        }
    }

    private static class CollectionConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            String rawName = targetType.getName();
            return options.mapCollections()
                && ("java.util.List".equals(rawName) || "java.util.Set".equals(rawName))
                && needsItemConversion(itemType(targetType));
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            String helperMethod = "java.util.Set".equals(targetType.getName()) ? "mapSet" : "mapList";
            Expression itemConversion = convertValue(itemType(targetType), varX("it"), options);
            return castX(plain(targetType),
                callX(new ClassExpression(plain(XmMapSupport.class)), helperMethod,
                    args(value, closureOfIt(itemConversion))));
        }
    }

    private static class MapFieldConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            return options.mapCollections()
                && "java.util.Map".equals(targetType.getName())
                && needsItemConversion(mapValueType(targetType));
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            Expression valueConversion = convertValue(mapValueType(targetType), varX("it"), options);
            return castX(plain(targetType),
                callX(new ClassExpression(plain(XmMapSupport.class)), "mapMap",
                    args(value, closureOfIt(valueConversion))));
        }
    }

    /** terminal converter: plain cast, supports every remaining type */
    private static class DefaultCastConverter implements XmMapValueConverter {

        @Override
        public boolean isSupport(ClassNode targetType, ConversionOptions options) {
            return true;
        }

        @Override
        public Expression convert(ClassNode targetType, Expression value, ConversionOptions options) {
            ClassNode castTarget = ClassHelper.isPrimitiveType(targetType)
                ? ClassHelper.getWrapper(targetType).getPlainNodeReference()
                : plain(targetType);
            return new CastExpression(castTarget, value);
        }
    }

    static ClassNode plain(Class<?> type) {
        return ClassHelper.make(type).getPlainNodeReference();
    }

    static ClassNode plain(ClassNode type) {
        return type.getPlainNodeReference();
    }

    /**
     * groovy marks property backing fields as synthetic, so isSynthetic() cannot be used here —
     * the internal fields ($staticClassInfo, __$stMC, metaClass) are recognized by name instead
     */
    static boolean isSkippedField(FieldNode field, List<String> excludes) {
        String name = field.getName();
        return field.isStatic()
            || name.contains("$")
            || "metaClass".equals(name)
            || excludes.contains(name);
    }

    static boolean isParseableJavaTime(ClassNode type) {
        return PARSEABLE_JAVA_TIME.contains(type.getName());
    }

    /** works both for source ClassNodes in the same compilation unit and for resolved precompiled classes */
    static boolean isXmMapConstructor(ClassNode type) {
        return type.getAnnotations().stream()
            .anyMatch(a -> a.getClassNode().getName().equals(XmMapConstructor.class.getCanonicalName()));
    }

    /** works both for source ClassNodes in the same compilation unit and for resolved precompiled classes */
    static boolean isXmToMap(ClassNode type) {
        return type.getAnnotations().stream()
            .anyMatch(a -> a.getClassNode().getName().equals(XmToMap.class.getCanonicalName()));
    }

    /**
     * Extracts a {@code @XmMapConvert} closure member from the field and strips it from the
     * runtime annotation. Returns null when the annotation or the member is absent.
     */
    static ClosureExpression extractConvertClosure(FieldNode field, String memberName) {
        return field.getAnnotations().stream()
            .filter(a -> a.getClassNode().getName().equals(XmMapConvert.class.getCanonicalName()))
            .findFirst()
            .map(a -> {
                Expression member = a.getMember(memberName);
                a.getMembers().remove(memberName);
                return member instanceof ClosureExpression closure ? closure : null;
            })
            .orElse(null);
    }

    static ClassNode itemType(ClassNode collectionType) {
        GenericsType[] generics = collectionType.getGenericsTypes();
        return (generics != null && generics.length == 1) ? generics[0].getType() : null;
    }

    static ClassNode mapValueType(ClassNode mapType) {
        GenericsType[] generics = mapType.getGenericsTypes();
        return (generics != null && generics.length == 2) ? generics[1].getType() : null;
    }

    /** conversion is generated per item only when the item type itself needs conversion */
    static boolean needsItemConversion(ClassNode type) {
        return type != null && (isParseableJavaTime(type) || isUuid(type) || type.isEnum() || isXmMapConstructor(type));
    }

    static boolean isUuid(ClassNode type) {
        return "java.util.UUID".equals(type.getName());
    }

    /** dynamic static-method call through a class expression: EnumType.fromValue((String) it) */
    static Expression staticCall(ClassNode type, String method, Expression argument) {
        return callX(new ClassExpression(plain(type)), method, args(argument));
    }

    static Expression castToString(Expression value) {
        return castX(plain(String.class), value);
    }

    /** single-statement closure { <expr(it)> } with its own scope; scopes are fixed by VariableScopeVisitor */
    static ClosureExpression closureOfIt(Expression expression) {
        ClosureExpression closure = new ClosureExpression(Parameter.EMPTY_ARRAY, returnS(expression));
        closure.setVariableScope(new VariableScope());
        return closure;
    }
}
