package com.icthh.xm.commons.lep.groovy.annotation;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION;

@Slf4j
@GroovyASTTransformation(phase = CANONICALIZATION)
public class LepServiceTransformation extends AbstractASTTransformation {

    private volatile static Map<String, List<String>> LEP_CONTEXT_FIELDS;

    public static void init(Class<? extends BaseLepContext> lepContextClass) {
        Map<String, List<String>> fields = new HashMap<>();
        fields.putAll(getFields("", BaseLepContext.class));
        fields.putAll(getFields("", lepContextClass));
        fields.put(BaseLepContext.class.getCanonicalName(), List.of("with{it}"));
        if (lepContextClass.getCanonicalName() != null) {
            fields.put(lepContextClass.getCanonicalName(), List.of("with{it}"));
        }

        LEP_CONTEXT_FIELDS = Map.copyOf(fields);
    }

    private static Map<String, List<String>> getFields(String basePath, Class<?> type) {
        Set<Class<?>> memberClasses = new HashSet<>(List.of(type.getNestMembers()));
        Map<String, List<String>> fields = new HashMap<>();
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
                    fields.putAll(getFields(field.getName() + ".", field.getType()));
                });
        return fields;
    }

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode classNode = (ClassNode) nodes[1];

        boolean isLepServiceFactoryEnabled = isLepServiceFactoryEnabled(nodes, classNode, source);

        StringBuilder body = generateFieldAssignment(classNode, isLepServiceFactoryEnabled);
        var constructor = findExistingLepConstructor(classNode);

        String mockClass = "class A { public A(Object lepContext) { \n " + body + " \n } }";
        var ast = new AstBuilder().buildFromString(CANONICALIZATION, false, mockClass);
        ConstructorNode generatedConstructor = ((ClassNode) ast.get(1)).getDeclaredConstructors().get(0);
        if (constructor.isPresent() && constructor.get().getParameters().length > 0) {
            ConstructorNode original = constructor.get();
            original.setCode(new BlockStatement(List.of(
                generatedConstructor.getCode(),
                original.getCode()
            ), new VariableScope()));
        } else if (constructor.isPresent() && constructor.get().getParameters().length == 0) {
            ConstructorNode original = constructor.get();
            classNode.removeConstructor(constructor.get());
            generatedConstructor.setCode(new BlockStatement(List.of(
                generatedConstructor.getCode(),
                original.getCode()
            ), new VariableScope()));
            classNode.addConstructor(generatedConstructor);
        } else {
            classNode.addConstructor(generatedConstructor);
        }
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

    private StringBuilder generateFieldAssignment(ClassNode classNode, boolean isLepServiceFactoryEnabled) {

        StringBuilder body = new StringBuilder();
        MutableLong serviceIndex = new MutableLong(0);
        classNode.getFields().forEach(field -> {
            if (canBeInjected(field) && isInLepContext(field)) {
                generateFieldFromLepContextAssigment(body, field);
            } else if (canBeInjected(field) && isLepService(field)) {
                serviceIndex.increment();
                generateLepServiceCreations(body, field, serviceIndex.intValue(), isLepServiceFactoryEnabled);
            }
        });
        return body;
    }

    private static boolean canBeInjected(FieldNode field) {
        return !field.isStatic() && (field.isFinal() || isAnnotated(field, LepInject.class)) && !isAnnotated(field, LepIgnoreInject.class);
    }

    private static boolean isAnnotated(AnnotatedNode node, Class<? extends Annotation> annotationClass) {
        return node.getAnnotations().stream().anyMatch(it -> it.getClassNode().getName().equals(annotationClass.getCanonicalName()));
    }

    private void generateLepServiceCreations(StringBuilder body, FieldNode field, int serviceNumber, boolean isLepServiceFactoryEnabled) {
        // TODO refactor to avoid reflection and support no args constructor
        String serviceVarName = "service_" + serviceNumber;
        body.append("String ").append(serviceVarName).append(" = ")
                .append("'").append(field.getType().getName()).append("'\n");
        body.append("Class ").append(serviceVarName).append("_class = Class.forName(")
                .append(serviceVarName).append(")\n");
        if (isLepServiceFactoryEnabled) {
            body.append("this.").append(field.getName())
                    .append(" = lepContext.lepServices.getInstance(").append(serviceVarName).append("_class")
                    .append(")\n");
        } else {
            body.append("this.").append(field.getName())
                    .append(" = ").append(serviceVarName).append("_class.getDeclaredConstructor(Object.class).newInstance(lepContext)")
                    .append("\n");
        }

    }

    private void generateFieldFromLepContextAssigment(StringBuilder body, FieldNode field) {
        String lepContextFieldName = getFieldName(field);
        body.append("this.").append(field.getName())
                .append(" = lepContext.").append(lepContextFieldName)
                .append("\n");
    }

    private Optional<ConstructorNode> findExistingLepConstructor(ClassNode classNode) {
        return classNode.getDeclaredConstructors().stream().filter(it -> {
            boolean isNeededConstructor = false;
            if (it.getParameters().length == 0) {
                isNeededConstructor = true;
            } else if (it.getParameters().length == 1) {
                Parameter parameter = it.getParameters()[0];
                ClassNode parameterType = parameter.getType();
                isNeededConstructor = parameter.getName().equals("lepContext") && (
                    typeEqual(parameterType, classNode.getName())
                        || typeEqual(parameterType, Object.class.getCanonicalName())
                        || typeEqual(parameterType, Map.class.getCanonicalName()
                    )
                );
            }
            if (!isNeededConstructor) {
                log.warn("Constructor {} is not suitable for LepConstructor annotation. " +
                    "This must be constructor with exactly one argument with name \"lepContext\"", it);
            }
            return isNeededConstructor;
        }).findFirst();
    }

    private boolean typeEqual(ClassNode classNode, String type) {
        return classNode.getName().equals(type);
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
