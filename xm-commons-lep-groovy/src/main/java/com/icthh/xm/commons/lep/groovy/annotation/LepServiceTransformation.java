package com.icthh.xm.commons.lep.groovy.annotation;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;
import static org.codehaus.groovy.ast.expr.ArgumentListExpression.EMPTY_ARGUMENTS;
import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION;

@Slf4j
@GroovyASTTransformation(phase = CANONICALIZATION)
public class LepServiceTransformation extends AbstractASTTransformation {

    private volatile static Map<String, List<String>> LEP_CONTEXT_FIELDS;
    private volatile static Set<String> LEP_CONTEXT_TYPE_HIERARCHY; // all classes and interfaces in hierarchy
    private volatile static Set<String> LEP_CONTEXT_CLASS_HIERARCHY; // all classes to BaseLepContext

    public static final String LEP_CONTEXT_NAME = "lepContext";

    public static void init(Class<? extends BaseLepContext> lepContextClass) {
        Set<Class<?>> typeHierarchy = buildTypeHierarchy(lepContextClass);
        Set<Class<?>> classHierarchy = typeHierarchy.stream()
            .filter(BaseLepContext.class::isAssignableFrom)
            .collect(toSet());


        Map<String, List<String>> fields = new HashMap<>();
        classHierarchy.forEach(type -> getFields(fields, "", type));
        String canonicalName = lepContextClass.getCanonicalName();
        if (canonicalName != null) {
            fields.put(canonicalName, List.of("with{it}"));
        }

        typeHierarchy.forEach(it -> fields.putIfAbsent(it.getCanonicalName(), List.of("with{it}")));

        LEP_CONTEXT_CLASS_HIERARCHY = Set.copyOf(toCanonicalNames(classHierarchy));
        LEP_CONTEXT_TYPE_HIERARCHY = Set.copyOf(toCanonicalNames(typeHierarchy));
        LEP_CONTEXT_FIELDS = Map.copyOf(fields);
    }

    private static Set<String> toCanonicalNames(Set<Class<?>> typeHierarchy) {
        return typeHierarchy.stream().map(Class::getCanonicalName).collect(Collectors.toSet());
    }

    private static Set<Class<?>> buildTypeHierarchy(Class<? extends BaseLepContext> lepContextClass) {
        Set<Class<?>> typeHierarchy = new HashSet<>();
        Class<?> currentClass = lepContextClass;
        while (currentClass != null) {
            typeHierarchy.add(currentClass);
            typeHierarchy.addAll(Arrays.asList(currentClass.getInterfaces()));
            currentClass = currentClass.getSuperclass();
        }
        return typeHierarchy;
    }

    private static void getFields(Map<String, List<String>> fields, String basePath, Class<?> type) {
        Set<Class<?>> memberClasses = new HashSet<>(List.of(type.getNestMembers()));
        stream(type.getFields())
                .filter(field -> !field.getType().isAssignableFrom(Object.class))
                .filter(field -> !memberClasses.contains(field.getType()))
                .forEach(field -> {
                    fields.putIfAbsent(field.getType().getCanonicalName(), new ArrayList<>());
                    fields.get(field.getType().getCanonicalName()).add(basePath + field.getName());
                });
        stream(type.getFields())
                .filter(field -> memberClasses.contains(field.getType()))
                .forEach(field -> {
                    getFields(fields, field.getName() + ".", field.getType());
                });
    }

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        try {
            internalVisit(nodes, source);
        } catch (Throwable e) {
            log.error("Error during LepServiceTransformation", e);
            throw e;
        }
    }

    private void internalVisit(ASTNode[] nodes, SourceUnit source) {
        ClassNode classNode = (ClassNode) nodes[1];

        boolean isLepServiceFactoryEnabled = isLepServiceFactoryEnabled(nodes, classNode, source);
        List<Statement> statements = generateFieldAssignment(classNode, isLepServiceFactoryEnabled);
        var constructor = findExistingLepConstructor(classNode);

        String mockClass = "\n class A { public A(def lepContext) {  } }";
        var ast = new AstBuilder().buildFromString(CANONICALIZATION, true, mockClass);
        ConstructorNode generatedConstructor = ((ClassNode) ast.get(1)).getDeclaredConstructors().get(0);
        generatedConstructor.setCode(new BlockStatement(statements, new VariableScope()));

        if (constructor.isPresent() && constructor.get().getParameters().length > 0) {
            ConstructorNode original = constructor.get();
            var constructorStatements = new ArrayList<Statement>(statements);
            constructorStatements.add(original.getCode());
            original.setCode(new BlockStatement(constructorStatements, new VariableScope()));
        } else if (constructor.isPresent() && constructor.get().getParameters().length == 0) {
            ConstructorNode original = constructor.get();
            var constructorStatements = new ArrayList<Statement>(statements);
            constructorStatements.add(original.getCode());
            generatedConstructor.setCode(new BlockStatement(constructorStatements, new VariableScope()));
            classNode.removeConstructor(constructor.get());
            classNode.addConstructor(generatedConstructor);
        } else {
            classNode.addConstructor(generatedConstructor);
        }

    }

    private Statement createAssignment(String fieldName, String rhsExpression) {
        Expression thisExpression = new VariableExpression("this");
        Expression fieldExpression = new PropertyExpression(thisExpression, fieldName);
        Expression rhsExpr = createExpression(rhsExpression);

        return new ExpressionStatement(new BinaryExpression(
            fieldExpression,
            Token.newSymbol("=", -1, -1),
            rhsExpr
        ));
    }

    private Statement createServiceInstance(FieldNode field) {
        Expression thisExpression = new VariableExpression("this");
        Expression fieldExpression = new PropertyExpression(thisExpression, field.getName());

        List<ConstructorNode> constructors = field.getType().getDeclaredConstructors();

        if (constructors.stream().anyMatch(this::isLepContextConstructor) || isAnnotated(field.getType(), LepConstructor.class)) {
            return callLepConstructor(field, fieldExpression);
        }

        if (constructors.isEmpty() || hasEmptyArgumentConstructor(field.getType())) {
            return new ExpressionStatement(new BinaryExpression(
                fieldExpression,
                Token.newSymbol("=", -1, -1),
                new ConstructorCallExpression(field.getType(), EMPTY_ARGUMENTS)
            ));
        }

        log.error("No suitable class constructor found for field {}", field.getName());
        return callLepConstructor(field, fieldExpression);
    }

    private static ExpressionStatement callLepConstructor(FieldNode field, Expression fieldExpression) {
        Expression classConstructorCall = new ConstructorCallExpression(
            field.getType(),
            new ArgumentListExpression(new VariableExpression(LEP_CONTEXT_NAME))
        );

        return new ExpressionStatement(new BinaryExpression(
            fieldExpression,
            Token.newSymbol("=", -1, -1),
            classConstructorCall
        ));
    }

    private static boolean hasEmptyArgumentConstructor(ClassNode type) {
        return type.getDeclaredConstructors().stream().anyMatch(it -> it.getParameters().length == 0);
    }

    private Expression createExpression(String expression) {
        String[] parts = expression.split("\\.");
        Expression result = new VariableExpression(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = new PropertyExpression(result, parts[i]);
        }
        return result;
    }

    private static boolean isLepServiceFactoryEnabled(ASTNode[] nodes, ClassNode classNode, SourceUnit source) {
        AnnotationNode annotationNode = null;
        for (ASTNode node : nodes) {
            if (node instanceof AnnotationNode && ((AnnotationNode) node).getClassNode().getName().equals(
                    LepConstructor.class.getCanonicalName()
            )) {
                annotationNode = (AnnotationNode) node;
                break;
            }
        }

        if (annotationNode != null) {
            // Get the boolean attribute value by name
            var expression = annotationNode.getMember("useLepFactory");
            if (expression instanceof ConstantExpression) {
                return Boolean.parseBoolean(((ConstantExpression) expression).getValue().toString());
            }
        }
        return true;
    }

    private List<Statement> generateFieldAssignment(ClassNode classNode, boolean isLepServiceFactoryEnabled) {
        List<Statement> statements = new ArrayList<>();
        classNode.getFields().forEach(field -> {
            if (LEP_CONTEXT_CLASS_HIERARCHY.contains(field.getType().getName()) || lepContextConventionField(field)) {
                statements.add(createAssignment(field.getName(), LEP_CONTEXT_NAME));
            } else if (canBeInjected(field) && isInLepContext(field)) {
                generateFieldFromLepContextAssigment(statements, field);
            } else if (canBeInjected(field) && isLepService(field)) {
                generateLepServiceCreations(statements, classNode, field, isLepServiceFactoryEnabled);
            }
        });
        return statements;
    }

    private static boolean lepContextConventionField(FieldNode field) {
        return field.getName().equals(LEP_CONTEXT_NAME) && (field.isDynamicTyped() || LEP_CONTEXT_TYPE_HIERARCHY.contains(field.getType().getName()));
    }

    private static boolean canBeInjected(FieldNode field) {
        return !field.isStatic() && (field.isFinal() || isAnnotated(field, LepInject.class)) && !isAnnotated(field, LepIgnoreInject.class);
    }

    private static boolean isAnnotated(AnnotatedNode node, Class<? extends Annotation> annotationClass) {
        return node.getAnnotations().stream().anyMatch(it -> it.getClassNode().getName().equals(annotationClass.getCanonicalName()));
    }

    private void generateLepServiceCreations(List<Statement> statements, ClassNode classNode, FieldNode field, boolean isLepServiceFactoryEnabled) {

        this.addImportToClass(classNode, field);

        if (isLepServiceFactoryEnabled) {
            statements.add(createServiceInstanceUsingGetInstance(field));
        } else {
            statements.add(createServiceInstance(field));
        }
    }

    private Statement createServiceInstanceUsingGetInstance(FieldNode field) {
        Expression thisExpression = new VariableExpression("this");
        Expression fieldExpression = new PropertyExpression(thisExpression, field.getName());
        Expression lepContextExpression = new VariableExpression(LEP_CONTEXT_NAME);
        Expression lepServicesExpression = new PropertyExpression(lepContextExpression, "lepServices");

        Expression getInstanceMethodCall = new MethodCallExpression(
            lepServicesExpression,
            "getInstance",
            new ArgumentListExpression(new ClassExpression(field.getType()))
        );

        return new ExpressionStatement(new BinaryExpression(
            fieldExpression,
            Token.newSymbol("=", -1, -1),
            getInstanceMethodCall
        ));
    }

    private void addImportToClass(ClassNode classNode, FieldNode field) {
        ModuleNode moduleNode = classNode.getModule();
        if (moduleNode == null) {
            return;
        }

        // Check if the import already exists
        String typeName = field.getType().getName();
        boolean importExists = moduleNode.getImports().stream()
            .anyMatch(importNode -> typeName.equals(importNode.getClassName()));
        if (importExists) {
            return;
        }

        moduleNode.addImport(typeName, field.getType());
    }

    private void generateFieldFromLepContextAssigment(List<Statement> statements, FieldNode field) {
        String lepContextFieldName = LEP_CONTEXT_NAME + "." + getFieldName(field);
        statements.add(createAssignment(field.getName(), lepContextFieldName));
    }

    private Optional<ConstructorNode> findExistingLepConstructor(ClassNode classNode) {
        return classNode.getDeclaredConstructors().stream().filter(it -> {
            boolean isNeededConstructor = false;
            if (it.getParameters().length == 0) {
                isNeededConstructor = true;
            } else if (it.getParameters().length == 1) {
                Parameter parameter = it.getParameters()[0];
                isNeededConstructor = parameter.getName().equals(LEP_CONTEXT_NAME) && isLepContextConstructor(it);
            }
            if (!isNeededConstructor) {
                log.warn("Constructor {} is not suitable for LepConstructor annotation. " +
                    "This must be constructor with exactly one argument with name \"lepContext\"", it);
            }
            return isNeededConstructor;
        }).findFirst();
    }

    private boolean isLepContextConstructor(ConstructorNode constructorNode) {
        if (constructorNode.getParameters().length == 1) {
            Parameter parameter = constructorNode.getParameters()[0];
            ClassNode parameterType = parameter.getType();
            return LEP_CONTEXT_TYPE_HIERARCHY.contains(parameterType.getName());
        }
        return false;
    }

    private boolean isLepService(FieldNode field) {
        return isAnnotated(field.getType(), LepConstructor.class) || isAnnotated(field.getType(), LepInjectableService.class);
    }

    private boolean isInLepContext(FieldNode field) {
        return LEP_CONTEXT_FIELDS.containsKey(field.getType().getName());
    }

    private String getFieldName(FieldNode field) {
        List<String> candidates = LEP_CONTEXT_FIELDS.get(field.getType().getName());
        String lepContextFieldName = candidates.get(0);
        if (candidates.size() > 1) {
            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
            lepContextFieldName = candidates.stream().min(comparing(name ->
                levenshteinDistance.apply(name.toLowerCase(), field.getName().toLowerCase())
            )).get();
        }
        return lepContextFieldName;
    }

}
