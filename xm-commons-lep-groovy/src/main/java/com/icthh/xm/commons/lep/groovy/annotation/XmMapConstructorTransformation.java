package com.icthh.xm.commons.lep.groovy.annotation;

import com.icthh.xm.commons.lep.groovy.annotation.XmMapValueConverter.ConversionOptions;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.convertValue;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.extractConvertClosure;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.isSkippedField;
import static com.icthh.xm.commons.lep.groovy.annotation.XmMapAstSupport.plain;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.block;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.notNullX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.params;
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION;

@GroovyASTTransformation(phase = CANONICALIZATION)
public class XmMapConstructorTransformation extends AbstractASTTransformation {

    static final String MAP_PARAM = "map";

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotationNode anno = (AnnotationNode) nodes[0];
        ClassNode classNode = (ClassNode) nodes[1];

        if (hasDeclaredMapConstructor(classNode)) {
            addError("@XmMapConstructor: class already declares a Map constructor", classNode);
            return;
        }

        List<String> excludes = getMemberStringList(anno, "excludes");
        List<String> excludesSafe = excludes == null ? List.of() : excludes;
        ConversionOptions options = new ConversionOptions(
            memberBoolean(anno, "mapChildClasses"), memberBoolean(anno, "mapCollections"));
        boolean noArgConstructor = memberBoolean(anno, "noArgConstructor");

        Statement preStatement = extractClosureBody(anno, "pre");
        Statement postStatement = extractClosureBody(anno, "post");

        List<Statement> body = new ArrayList<>();
        ClassNode superClass = classNode.getSuperClass();
        if (superClass != null && !ClassHelper.OBJECT_TYPE.equals(superClass)
            && (XmMapAstSupport.isXmMapConstructor(superClass) || hasDeclaredMapConstructor(superClass))) {
            body.add(stmt(new ConstructorCallExpression(ClassNode.SUPER, args(varX(MAP_PARAM)))));
        }
        if (preStatement != null) {
            body.add(preStatement);
        }
        body.add(ifS(notNullX(varX(MAP_PARAM)), mappingBlock(classNode, excludesSafe, options)));
        if (postStatement != null) {
            body.add(postStatement);
        }

        Parameter mapParameter = param(plain(Map.class), MAP_PARAM);
        classNode.addConstructor(PUBLIC, params(mapParameter), ClassNode.EMPTY_ARRAY,
            new BlockStatement(body, null));

        if (noArgConstructor && !hasFinalFields(classNode)
            && classNode.getDeclaredConstructor(Parameter.EMPTY_ARRAY) == null
            && classNode.getDeclaredConstructors().size() == 1) {
            classNode.addConstructor(PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, block());
        }

        new VariableScopeVisitor(sourceUnit).visitClass(classNode);
    }

    private Statement mappingBlock(ClassNode classNode, List<String> excludes, ConversionOptions options) {
        List<Statement> statements = new ArrayList<>();
        for (FieldNode field : classNode.getFields()) {
            if (isSkippedField(field, excludes)) {
                continue;
            }
            String name = field.getName();
            VariableExpression valueVar = varX(name + "Obj");
            statements.add(declS(valueVar, callX(varX(MAP_PARAM), "get", constX(name))));
            ClosureExpression fromClosure = extractConvertClosure(field, "from");
            Expression converted = fromClosure != null
                ? callX(fromClosure, "call", args(varX(name + "Obj")))
                : convertValue(field.getType(), varX(name + "Obj"), options);
            statements.add(ifS(notNullX(varX(name + "Obj")),
                assignS(propX(varX("this"), name), converted)));
        }
        return new BlockStatement(statements, null);
    }

    /** a default constructor would leave final fields unassigned, so it is not generated for such classes */
    private static boolean hasFinalFields(ClassNode classNode) {
        return classNode.getFields().stream()
            .anyMatch(field -> field.isFinal() && !isSkippedField(field, List.of()));
    }

    /** inlines the closure body into the constructor and strips the member from the runtime annotation */
    private Statement extractClosureBody(AnnotationNode anno, String memberName) {
        Expression member = anno.getMember(memberName);
        anno.getMembers().remove(memberName);
        if (member instanceof ClosureExpression closure) {
            return closure.getCode();
        }
        return null;
    }

    private boolean memberBoolean(AnnotationNode anno, String name) {
        Expression member = anno.getMember(name);
        if (member instanceof ConstantExpression constant) {
            return !Boolean.FALSE.equals(constant.getValue());
        }
        return true;
    }

    private static boolean hasDeclaredMapConstructor(ClassNode classNode) {
        return classNode.getDeclaredConstructors().stream()
            .anyMatch(ctor -> ctor.getParameters().length == 1
                && ctor.getParameters()[0].getType().getName().equals("java.util.Map"));
    }
}
