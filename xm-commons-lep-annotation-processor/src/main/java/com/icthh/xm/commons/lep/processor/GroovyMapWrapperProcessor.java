package com.icthh.xm.commons.lep.processor;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.function.Predicate.not;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@SupportedAnnotationTypes("com.icthh.xm.commons.lep.processor.GroovyMap")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class GroovyMapWrapperProcessor extends AbstractProcessor {

    private Types typeUtils;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
    }

    private Set<String> listFields(TypeElement typeElement) {
        Set<String> fields = new HashSet<>();
        while (typeElement != null && !isObjectClass(typeElement)) {
            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == FIELD && enclosedElement.getModifiers().contains(PUBLIC)) {
                    fields.add(enclosedElement.getSimpleName().toString());
                }
            }
            TypeMirror superclass = typeElement.getSuperclass();
            typeElement = (TypeElement) typeUtils.asElement(superclass);
        }
        return fields;
    }

    private boolean isObjectClass(TypeElement typeElement) {
        return typeElement.toString().equals(Object.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GroovyMap.class)) {
            if (!(element instanceof TypeElement)) {
                processingEnv.getMessager().printMessage(ERROR, "Can only be applied to class.");
                return true;
            }

            TypeElement baseElement = (TypeElement) element;
            String packageName = processingEnv.getElementUtils().getPackageOf(baseElement).getQualifiedName().toString();
            ClassName baseClassName = ClassName.get(packageName, baseElement.getSimpleName().toString());

            // Define the wrapper class builder
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder("GroovyMapLepContextWrapper")
                .addSuperinterface(ParameterizedTypeName.get(Map.class, String.class, Object.class))
                .superclass(baseClassName)
                .addModifiers(PUBLIC);

            Set<String> fields = listFields(baseElement);

            // Constructor
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addParameter(BaseLepContext.class, "baseLepContext")
                .addModifiers(PUBLIC);
            constructorBuilder.addStatement("$L lepContext = ($L)baseLepContext", baseClassName, baseClassName);
            fields.forEach(field -> constructorBuilder.addStatement("this.$L = lepContext.$L", field, field));
            constructorBuilder.addStatement("lepContext.setAdditionalContextTo(this)");
            MethodSpec constructor = constructorBuilder.build();
            classBuilder.addMethod(constructor);

            // Override get method
            MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder("get")
                .addAnnotation(Override.class)
                .returns(Object.class)
                .addParameter(Object.class, "key")
                .addModifiers(PUBLIC)
                .beginControlFlow("switch (key.toString())");
            fields.forEach(field -> getMethodBuilder.addCode("case \"$L\": return this.$L;\n", field, field));
            classBuilder.addMethod(getMethodBuilder
                .addCode("default: return this.getAdditionalContext(key.toString());\n")
                .endControlFlow()
                .build());

            Method[] methods = Map.class.getMethods();
            Arrays.stream(methods)
                .filter(not(Method::isDefault))
                .filter(it -> !isStatic(it.getModifiers()))
                .filter(it -> !"get".equals(it.getName()))
                .filter(it -> !"put".equals(it.getName()))
                .forEach(method -> {
                    MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName())
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .returns(method.getReturnType());
                    Arrays.stream(method.getParameters()).forEach(param -> {
                        builder.addParameter(param.getType(), param.getName());
                    });
                    MethodSpec methodSpec = builder
                    .addStatement("throw new UnsupportedOperationException(\"Not implemented\")")
                    .build();
                classBuilder.addMethod(methodSpec);
            });

            MethodSpec putMethod = MethodSpec.methodBuilder("put")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(Object.class)
                .addParameter(String.class, "key")
                .addParameter(Object.class, "value")
                .addStatement("throw new UnsupportedOperationException(\"Not implemented\")")
                .build();
            classBuilder.addMethod(putMethod);

            JavaFile javaFile = JavaFile.builder("com.icthh.xm.commons", classBuilder.build()).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(ERROR, "Failed to write generated class: " + e.getMessage());
            }
        }
        return true;
    }
}
