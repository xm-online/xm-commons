package com.icthh.xm.commons.lep.groovy.annotation;

import com.icthh.xm.commons.lep.groovy.XmMapSupport;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.closureOfIt;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.extractConvertClosure;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.isParseableJavaTime;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.isSkippedField;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.isUuid;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.isXmToMap;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.itemType;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.mapValueType;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.plain;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callSuperX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.notNullX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION;

@GroovyASTTransformation(phase = CANONICALIZATION)
public class XmToMapTransformation extends AbstractASTTransformation {

    private static final String RESULT = "map";

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        ClassNode classNode = (ClassNode) nodes[1];

        if (!classNode.getDeclaredMethods("toMap").isEmpty()) {
            addError("@XmToMap: class already declares a toMap method", classNode);
            return;
        }

        List<Statement> body = new ArrayList<>();
        body.add(declS(varX(RESULT), ctorX(plain(LinkedHashMap.class))));

        ClassNode superClass = classNode.getSuperClass();
        if (superClass != null && !ClassHelper.OBJECT_TYPE.equals(superClass) && isXmToMap(superClass)) {
            body.add(stmt(callX(varX(RESULT), "putAll", args(callSuperX("toMap")))));
        }

        for (FieldNode field : classNode.getFields()) {
            if (isSkippedField(field, List.of())) {
                continue;
            }
            body.add(serializeField(field));
        }

        body.add(returnS(varX(RESULT)));

        classNode.addMethod("toMap", PUBLIC, plain(Map.class), Parameter.EMPTY_ARRAY,
            ClassNode.EMPTY_ARRAY, new BlockStatement(body, null));

        new VariableScopeVisitor(sourceUnit).visitClass(classNode);
    }

    private Statement serializeField(FieldNode field) {
        String name = field.getName();
        ClassNode type = field.getType();
        Expression fieldRef = propX(varX("this"), name);

        ClosureExpression toClosure = extractConvertClosure(field, "to");
        if (toClosure != null) {
            return putWhenNotNull(name, fieldRef, callX(toClosure, "call", args(fieldRef)));
        }
        if (isParseableJavaTime(type) || isUuid(type)) {
            return putWhenNotNull(name, fieldRef, callX(fieldRef, "toString"));
        }
        if (type.isEnum()) {
            String method = type.getMethods("value").isEmpty() ? "name" : "value";
            return putWhenNotNull(name, fieldRef, callX(fieldRef, method));
        }
        String rawName = type.getName();
        if ("java.util.List".equals(rawName) || "java.util.Set".equals(rawName)) {
            ClassNode item = itemType(type);
            Expression serialized = (item != null && isXmToMap(item))
                ? helperCall("toMapList", fieldRef, closureOfIt(callX(varX("it"), "toMap")))
                : helperCall("toMapList", fieldRef, null);
            return stmt(callX(varX(RESULT), "put", args(constX(name), serialized)));
        }
        if ("java.util.Map".equals(rawName)) {
            ClassNode valueType = mapValueType(type);
            if (valueType != null && isXmToMap(valueType)) {
                Expression serialized = helperCall("toMapMap", fieldRef, closureOfIt(callX(varX("it"), "toMap")));
                return stmt(callX(varX(RESULT), "put", args(constX(name), serialized)));
            }
            return stmt(callX(varX(RESULT), "put", args(constX(name), fieldRef)));
        }
        if (isXmToMap(type)) {
            return putWhenNotNull(name, fieldRef, callX(fieldRef, "toMap"));
        }
        return stmt(callX(varX(RESULT), "put", args(constX(name), fieldRef)));
    }

    private Statement putWhenNotNull(String name, Expression fieldRef, Expression serialized) {
        return ifS(notNullX(fieldRef),
            stmt(callX(varX(RESULT), "put", args(constX(name), serialized))));
    }

    private Expression helperCall(String method, Expression fieldRef, Expression closure) {
        Expression helper = new ClassExpression(plain(XmMapSupport.class));
        return closure == null
            ? callX(helper, method, args(fieldRef))
            : callX(helper, method, args(fieldRef, closure));
    }
}
