/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.common.visitor.generator;

import static com.google.auto.common.MoreElements.isAnnotationPresent;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.common.visitor.Context;
import com.google.j2cl.common.visitor.Visitable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * The J2clAstProcessor emits a single AbstractVisitor class and a Visitor helper class for each
 * {@code @Visitable} type.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class J2clAstProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(Visitable.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    ImmutableList<TypeElement> deferredTypes =
        deferredTypeNames.stream()
            .map(deferred -> processingEnv.getElementUtils().getTypeElement(deferred))
            .collect(toImmutableList());
    if (roundEnv.processingOver()) {
      // Create abstract visitor class

      for (String packageName : processedVisitableClassesByPackageName.keySet()) {

        PackageElement packageElement =
            processingEnv.getElementUtils().getPackageElement(packageName);
        ImmutableList<VisitableClass> classes =
            ElementFilter.typesIn(packageElement.getEnclosedElements()).stream()
                .filter(input -> isAnnotationPresent(input, Visitable.class))
                .map(this::extractVisitableClass)
                .collect(toImmutableList());

        writeGeneralClass(ABSTRACT_VISITOR_TEMPLATE_FILE, "AbstractVisitor", packageName, classes);
        writeGeneralClass(
            ABSTRACT_REWRITER_TEMPLATE_FILE, "AbstractRewriter", packageName, classes);
        writeGeneralClass(
            PROCESSOR_PRIVATE_CLASS_TEMPLATE_FILE, "ProcessorPrivate", packageName, classes);
      }

      // This means that the previous round didn't generate any new sources, so we can't have found
      // any new instances of @Visitable; and we can't have any new types that are the reason a type
      // was in deferredTypes.
      for (TypeElement type : deferredTypes) {
        reportError(
            "Did not generate @Visitable class for "
                + type.getQualifiedName()
                + " because it references undefined types",
            type);
      }
      return false;
    }
    Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Visitable.class);
    ImmutableList<TypeElement> types =
        new ImmutableList.Builder<TypeElement>()
            .addAll(deferredTypes)
            .addAll(ElementFilter.typesIn(annotatedElements))
            .build();
    deferredTypeNames.clear();

    for (TypeElement type : types) {
      try {
        processType(type);
      } catch (AbortProcessingException e) {
        // We abandoned this type; continue with the next.
      } catch (MissingTypeException e) {
        // We abandoned this type, but only because we needed another type that it references and
        // that other type was missing. It is possible that the missing type will be generated by
        // further annotation processing, so we will try again on the next round (perhaps failing
        // again and adding it back to the list). We save the name of the @Visitable type rather
        // than its TypeElement because it is not guaranteed that it will be represented by
        // the same TypeElement on the next round.
        deferredTypeNames.add(type.getQualifiedName().toString());
      } catch (RuntimeException e) {
        // Don't propagate this exception, which will confusingly crash the compiler.
        // Instead, report a compiler error with the stack trace.
        String trace = Throwables.getStackTraceAsString(e);
        reportError("@Visitable processor threw an exception: " + trace, type);
      }
    }

    return false; // never claim annotation, because who knows what other processors want?
  }

  /** Field represents a visitable field in a visitable class. */
  public static class Field {
    enum Type {
      SCALAR,
      LIST
    }

    String name;
    Type type;
    String typeName;
    String componentTypeName;

    boolean isNullable;

    Field(String name, TypeMirror type, boolean isNullable) {
      this.name = name;
      this.isNullable = isNullable;
      TypeElement typeElement = MoreElements.asType(MoreTypes.asElement(type));
      if (typeElement.getQualifiedName().contentEquals("java.util.List")) {
        this.type = Type.LIST;
        this.componentTypeName =
            MoreTypes.asElement(MoreTypes.asDeclared(type).getTypeArguments().get(0))
                .getSimpleName()
                .toString();
      } else {
        this.type = Type.SCALAR;
      }
      this.typeName = typeElement.getSimpleName().toString();
    }

    public String getName() {
      return name;
    }

    public String getTypeName() {
      return typeName;
    }

    public String getComponentTypeName() {
      return componentTypeName;
    }

    public boolean isList() {
      return type == Type.LIST;
    }

    public boolean isNullable() {
      return isNullable;
    }
  }

  /**
   * VisitableClass represents a class for which visitor code will be generated by this processor.
   */
  public static class VisitableClass {
    String packageName;
    String simpleName;
    String superclassName;
    String topClassName;
    List<Field> fields;
    boolean isContext;

    @Override
    public int hashCode() {
      return Objects.hashCode(packageName, simpleName);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof VisitableClass)) {
        return false;
      }
      VisitableClass testClass = (VisitableClass) other;
      boolean simpleNameEquals = Objects.equal(simpleName, testClass.simpleName);
      boolean packageNameEquals = Objects.equal(packageName, testClass.packageName);
      return simpleNameEquals && packageNameEquals;
    }

    public String getSimpleName() {
      return simpleName;
    }

    public String getParameterName() {
      return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, simpleName);
    }

    public boolean isTop() {
      return superclassName == null;
    }

    public boolean isContext() {
      return isContext;
    }

    public String getSuperclassName() {
      return superclassName;
    }

    public String getTopClassName() {
      return topClassName;
    }
  }

  private Map<String, ?> createVelocityContextForVisitorHelper(VisitableClass visitableClass) {
    Map<String, Object> vc = new HashMap<>();
    vc.put("className", visitableClass.simpleName);
    vc.put("packageName", visitableClass.packageName);
    vc.put("fields", visitableClass.fields);
    vc.put("visitableClass", visitableClass);
    return vc;
  }

  /**
   * Qualified names of {@code @AutoValue} classes that we attempted to process but had to abandon
   * because we needed other types that they referenced and those other types were missing.
   */
  private final List<String> deferredTypeNames = new ArrayList<>();

  private final SetMultimap<String, VisitableClass> processedVisitableClassesByPackageName =
      LinkedHashMultimap.create();

  private final VelocityRenderer velocityRenderer = new VelocityRenderer(getClass());

  private static final String ABSTRACT_VISITOR_TEMPLATE_FILE = "AbstractVisitorClass.vm";

  private static final String ABSTRACT_REWRITER_TEMPLATE_FILE = "AbstractRewriterClass.vm";

  private static final String PROCESSOR_PRIVATE_CLASS_TEMPLATE_FILE = "ProcessorPrivateClass.vm";

  private static final String VISITABLE_CLASS_TEMPLATE_FILE = "Visitable_Class.vm";

  public J2clAstProcessor() {}

  @Override
  public Set<String> getSupportedOptions() {
    return ImmutableSet.of();
  }

  private Map<String, ?> createVelocityContextForVisitor(
      String packageName, List<VisitableClass> visitableClasses) {
    Map<String, Object> vc = new HashMap<>();
    vc.put("classes", visitableClasses);
    vc.put("packageName", packageName);
    return vc;
  }

  private JavaFileObject createSourceFile(String claszzName) throws IOException {
    return processingEnv.getFiler().createSourceFile(claszzName);
  }

  private VisitableClass extractVisitableClass(final TypeElement typeElement) {
    ImmutableList<Field> allFieldsNames =
        ElementFilter.fieldsIn(typeElement.getEnclosedElements()).stream()
            .filter(hasAnnotation(Visitable.class))
            .map(
                variableElement -> {
                  TypeMirror fieldType = variableElement.asType();
                  return new Field(
                      variableElement.getSimpleName().toString(),
                      fieldType,
                      hasAnnotation(Nullable.class).apply(variableElement));
                })
            .collect(toImmutableList());

    if (!hasAcceptMethod(typeElement) && !typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
      abortWithError(
          typeElement.getQualifiedName()
              + " does not implement \""
              + " acceptInternal(Processor processor)\"",
          typeElement);
    }
    VisitableClass visitableClass = new VisitableClass();
    visitableClass.simpleName = typeElement.getSimpleName().toString();
    visitableClass.packageName = MoreElements.getPackage(typeElement).getQualifiedName().toString();
    visitableClass.fields = allFieldsNames;
    visitableClass.isContext = isAnnotationPresent(typeElement, Context.class);
    if (getSingleVisitableSuper(typeElement) != null) {
      visitableClass.superclassName =
          MoreTypes.asElement(getSingleVisitableSuper(typeElement)).getSimpleName().toString();
    }
    TypeElement topElement = typeElement;
    TypeMirror visitableSuper;
    while ((visitableSuper = getSingleVisitableSuper(topElement)) != null) {
      topElement = MoreElements.asType(MoreTypes.asElement(visitableSuper));
    }
    visitableClass.topClassName = topElement.getSimpleName().toString();
    return visitableClass;
  }

  private TypeMirror getSingleVisitableSuper(TypeElement typeElement) {
    ArrayList<TypeMirror> supers = new ArrayList<>();
    if (typeElement.getSuperclass() != null && isVisitable(typeElement.getSuperclass())) {
      supers.add(typeElement.getSuperclass());
    }
    supers.addAll(
        typeElement.getInterfaces().stream()
            .filter(J2clAstProcessor::isVisitable)
            .collect(toImmutableList()));
    if (supers.size() > 1) {
      abortWithError(
          typeElement.getQualifiedName() + " has more than one @Visitable direct supertype",
          typeElement);
    }
    return Iterables.getOnlyElement(supers, null);
  }

  private static boolean isVisitable(TypeMirror typeMirror) {
    return typeMirror != null
        && typeMirror.getKind() != TypeKind.NONE
        && MoreTypes.asElement(typeMirror).getAnnotationsByType(Visitable.class).length != 0;
  }

  private boolean hasAcceptMethod(final TypeElement typeElement) {
    return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .anyMatch(
            executableElement ->
                executableElement.getSimpleName().contentEquals("acceptInternal")
                    && executableElement.getParameters().size() == 1
                    && processingEnv
                        .getTypeUtils()
                        .asElement(executableElement.getParameters().get(0).asType())
                        .getSimpleName()
                        .contentEquals("Processor"));
  }

  private static Predicate<VariableElement> hasAnnotation(
      final Class<? extends Annotation> annotation) {
    return input -> isAnnotationPresent(input, annotation);
  }

  private void processType(TypeElement type) {
    VisitableClass visitableClass = extractVisitableClass(type);
    writeVisitableClassHelper(visitableClass);
    processedVisitableClassesByPackageName.put(visitableClass.packageName, visitableClass);
  }

  private void reportError(String string) {
    processingEnv.getMessager().printMessage(Kind.ERROR, string);
  }

  private void reportError(String string, TypeElement typeElement) {
    processingEnv.getMessager().printMessage(Kind.ERROR, string, typeElement);
  }

  private void abortWithError(String string, TypeElement typeElement) {
    processingEnv.getMessager().printMessage(Kind.ERROR, string, typeElement);
    throw new AbortProcessingException();
  }

  private void writeString(String clazzName, String content) throws IOException {
    JavaFileObject jfo = createSourceFile(clazzName);
    try (BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
      bw.write(content);
    }
  }

  private void writeVisitableClassHelper(VisitableClass visitableClass) {
    boolean success = false;
    try {
      String renderedTemplate =
          velocityRenderer.renderTemplate(
              VISITABLE_CLASS_TEMPLATE_FILE, createVelocityContextForVisitorHelper(visitableClass));
      writeString(
          visitableClass.packageName + ".Visitor_" + visitableClass.simpleName, renderedTemplate);
      success = true;
    } catch (IOException e) {
      success = false;
    } finally {
      if (!success) {
        reportError("Could not generate visitor for " + visitableClass.simpleName);
      }
    }
  }

  private void writeGeneralClass(
      String templateName,
      String className,
      String packageName,
      List<VisitableClass> visitableClasses) {

    boolean success = false;
    try {
      String renderedTemplate =
          velocityRenderer.renderTemplate(
              templateName, createVelocityContextForVisitor(packageName, visitableClasses));
      writeString(Joiner.on('.').join(packageName, className), renderedTemplate);
      success = true;
    } catch (Throwable e) {
      StringWriter stringWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stringWriter));
      reportError("Could not generate visitor class" + e + stringWriter);
      success = false;
    } finally {
      if (!success) {
        reportError("Could not generate visitor class");
      }
    }
  }
}
