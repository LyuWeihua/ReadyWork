package work.ready.core.tools.javapoet;

import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;

import static javax.lang.model.element.Modifier.FINAL;

public class JavaCodeBuilder {

    private String className;
    private String packageName;
    private JavaFile.Builder javaFileBuilder;
    private TypeSpec.Builder classBuilder;

    public JavaCodeBuilder(String packageName, String className) {
        this(packageName, className, new Modifier[0]);
    }

    public JavaCodeBuilder(String packageName, String className, Modifier... modifier) {
        this.className = className;
        this.packageName = packageName;
        this.classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(modifier);
    }

    public TypeSpec.Builder getClassBuilder() {
        return classBuilder;
    }

    public JavaFile.Builder getJavaFileBuilder() {
        if(this.javaFileBuilder == null) {
            this.javaFileBuilder = JavaFile.builder(packageName, classBuilder.build()).indent("    ");
        }
        return this.javaFileBuilder;
    }

    public MethodSpec mainMethod(String code) {
        return getMainMethodBuilder()
                .addCode(code)
                .build();
    }

    public MethodSpec mainMethod(String statement, Object... args) {
        return getMainMethodBuilder()
                .addStatement(statement, args)
                .build();
    }

    public void addMethods(MethodSpec... methodSpec) {
        if(methodSpec != null && methodSpec.length > 1) {
            classBuilder.addMethods(Arrays.asList(methodSpec));
        } else if(methodSpec != null && methodSpec.length == 1) {
            classBuilder.addMethod(methodSpec[0]);
        }
    }

    public void extendSuperClass(Class<?> superClass) {
        classBuilder.superclass(superClass);
        var constructors = superClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            var builder = copyFrom(constructor, false);
            List<String> parameters = new ArrayList<>();
            for (Parameter parameter : constructor.getParameters()) {
                parameters.add(parameter.getName());
            }
            builder.addCode("super($N);", StrUtil.join(parameters, ", "));
            classBuilder.addMethod(builder.build());
        }
    }

    public MethodSpec.Builder copyFrom(Executable referenceMethod) {
        return copyFrom(referenceMethod, false);
    }

    public MethodSpec.Builder copyFrom(Executable referenceMethod, boolean withAbstract) {
        MethodSpec.Builder builder;
        Set<String> typeVariable = new HashSet<>();
        if(referenceMethod instanceof Constructor) {
            builder = MethodSpec.constructorBuilder();
        } else {
            builder = MethodSpec.methodBuilder(referenceMethod.getName());
            Type returnType = ((Method)referenceMethod).getGenericReturnType();
            Set<Type> typeStore = new HashSet<>();
            ClassUtil.getAllGenericType(returnType, typeStore, true);
            typeStore.forEach(type -> {
                if(type instanceof TypeVariable) {
                    typeVariable.add(((TypeVariable<?>) type).getName());
                }
            });
            builder.returns(returnType);
        }
        for(Parameter parameter : referenceMethod.getParameters()) {
            Type paramType = parameter.getParameterizedType();

            Set<Type> typeStore = new HashSet<>();
            ClassUtil.getAllGenericType(paramType, typeStore, true);
            typeStore.forEach(type -> {
                if(type instanceof TypeVariable) {
                    typeVariable.add(((TypeVariable<?>) type).getName());
                }
            });

            if(java.lang.reflect.Modifier.isFinal(parameter.getModifiers())) {
                builder.addParameter(paramType, parameter.getName(), FINAL);
            } else {
                builder.addParameter(paramType, parameter.getName());
            }
        }
        typeVariable.forEach(type->builder.addTypeVariable(TypeVariableName.get(type)));
        String modifier = java.lang.reflect.Modifier.toString(referenceMethod.getModifiers());
        if(StrUtil.notBlank(modifier)) {
            String[] modifiers = StrUtil.split(modifier.toUpperCase(), ' ');
            for(int i = 0; i < modifiers.length; i++) {
                if(!withAbstract && "ABSTRACT".equals(modifiers[i])) {
                    continue;
                }
                builder.addModifiers(javax.lang.model.element.Modifier.valueOf(modifiers[i]));
            }
        }
        var annotations = referenceMethod.getDeclaredAnnotations();
        for(int i = 0; i < annotations.length; i++) {
            builder.addAnnotation(AnnotationSpec.get(annotations[i], true));
        }
        var exceptions = referenceMethod.getGenericExceptionTypes();
        for(int i = 0; i < exceptions.length; i++) {
            builder.addException(exceptions[i]);
        }
        return builder;
    }

    public File generateJavaFile(Path path) throws IOException {
        JavaFile javaFile = getJavaFileBuilder().build();
        return javaFile.writeToPath(path).toFile();
    }

    private MethodSpec.Builder getMainMethodBuilder() {
        return MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args");
    }
}
