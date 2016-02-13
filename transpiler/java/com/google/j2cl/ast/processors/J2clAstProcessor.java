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
package com.google.j2cl.ast.processors;

import static com.google.auto.common.MoreElements.isAnnotationPresent;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.common.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * The J2clAstProcessor emits a single AbstractVisitor class and a Visitor helper class for each
 * {@code @Visitable} type.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
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
    List<TypeElement> deferredTypes = new ArrayList<>();
    for (String deferred : deferredTypeNames) {
      deferredTypes.add(processingEnv.getElementUtils().getTypeElement(deferred));
    }
    if (roundEnv.processingOver()) {
      // Create abstract visitor class

      for (String packageName : processedVisitableClassesByPackageName.keySet()) {

        PackageElement packageElement =
            processingEnv.getElementUtils().getPackageElement(packageName);
        List<VisitableClass> classes =
            FluentIterable.from(ElementFilter.typesIn(packageElement.getEnclosedElements()))
                .filter(
                    new Predicate<TypeElement>() {
                      @Override
                      public boolean apply(TypeElement input) {
                        return isAnnotationPresent(input, Visitable.class);
                      }
                    })
                .transform(
                    new Function<TypeElement, VisitableClass>() {
                      @Override
                      public VisitableClass apply(TypeElement input) {
                        return extractVisitableClass(input);
                      }
                    })
                .toList();

        writeGeneralClass(ABSTRACT_VISITOR_TEMPLATE_FILE, "AbstractVisitor", packageName, classes);
        writeGeneralClass(
            ABSTRACT_REWRITER_TEMPLATE_FILE, "AbstractRewriter", packageName, classes);
        writeGeneralClass(
            ABSTRACT_TRANSFORMER_TEMPLATE_FILE, "AbstractTransformer", packageName, classes);
        writeGeneralClass(
            PROCESSOR_PRIVATE_CLASS_TEMPLATE_FILE, "ProcessorPrivate", packageName, classes);
        writeGeneralClass(VISITOR_INTERFACE_TEMPLATE_FILE, "Visitor", packageName, classes);
        writeGeneralClass(REWRITER_INTERFACE_TEMPLATE_FILE, "Rewriter", packageName, classes);
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
    Collection<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(Visitable.class);
    List<TypeElement> types =
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

  /**
   * Field represents a visitable filed in a visitable class.
   */
  public static class Field {
    enum Type {
      SCALAR,
      LIST;
    }

    String name;
    Type type;
    String typeName;

    boolean isNullable;

    Field(String name, TypeElement type, boolean isNullable) {
      this.name = name;
      this.isNullable = isNullable;
      if (type.getQualifiedName().contentEquals("java.util.List")) {
        this.type = Type.LIST;
      } else {
        this.type = Type.SCALAR;
      }
      this.typeName = type.getSimpleName().toString();
    }

    public String getName() {
      return name;
    }

    public String getTypeName() {
      return typeName;
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
    List<Field> fieldNames;
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

    public boolean isTop() {
      return superclassName == null;
    }

    public boolean isContext() {
      return isContext;
    }

    public String getSuperclassName() {
      return superclassName;
    }
  }

  private VelocityContext createVelocityContextForVisitorHelper(VisitableClass visitableClass) {
    VelocityContext vc = new VelocityContext();
    vc.put("className", visitableClass.simpleName);
    vc.put("packageName", visitableClass.packageName);
    vc.put("fields", visitableClass.fieldNames);
    vc.put("visitableClass", visitableClass);
    return vc;
  }

  /**
   * Qualified names of {@code @AutoValue} classes that we attempted to process but had to abandon
   * because we needed other types that they referenced and those other types were missing.
   */
  private final List<String> deferredTypeNames = new ArrayList<String>();

  private Multimap<String, VisitableClass> processedVisitableClassesByPackageName =
      LinkedHashMultimap.create();

  private static final String ABSTRACT_VISITOR_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/AbstractVisitorClass.vm";

  private static final String ABSTRACT_REWRITER_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/AbstractRewriterClass.vm";

  private static final String ABSTRACT_TRANSFORMER_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/AbstractTransformerClass.vm";

  private static final String PROCESSOR_PRIVATE_CLASS_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/ProcessorPrivateClass.vm";

  private static final String VISITOR_INTERFACE_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/VisitorInterface.vm";

  private static final String REWRITER_INTERFACE_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/RewriterInterface.vm";

  private static final String VISITABLE_CLASS_TEMPLATE_FILE =
      "com/google/j2cl/ast/processors/Visitable_Class.vm";

  private static final VelocityEngine velocityEngine = VelocityUtil.createEngine();

  public J2clAstProcessor() {}

  @Override
  public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  private VelocityContext createVelocityContextForVisitor(
      String packageName, List<VisitableClass> visitableClasses) {
    VelocityContext vc = new VelocityContext();
    vc.put("classes", visitableClasses);
    vc.put("packageName", packageName);
    return vc;
  }

  private JavaFileObject createSourceFile(String claszzName) throws IOException {
    return processingEnv.getFiler().createSourceFile(claszzName);
  }

  private VisitableClass extractVisitableClass(final TypeElement typeElement) {
    final Types typeUtils = processingEnv.getTypeUtils();
    ImmutableList<Field> allFieldsNames =
        FluentIterable.from(ElementFilter.fieldsIn(typeElement.getEnclosedElements()))
            .filter(hasAnnotation(Visitable.class))
            .transform(
                new Function<VariableElement, Field>() {
                  @Override
                  public Field apply(@Nullable VariableElement variableElement) {
                    TypeElement fieldTypeElement =
                        (TypeElement)
                            typeUtils.asElement(typeUtils.erasure(variableElement.asType()));
                    return new Field(
                        variableElement.getSimpleName().toString(),
                        fieldTypeElement,
                        hasAnnotation(Nullable.class).apply(variableElement));
                  }
                })
            .toList();

    if (!hasAcceptMethod(typeElement)) {
      abortWithError(
          typeElement.getQualifiedName()
              + " does not implement \""
              + " accept(Processor processor)\"",
          typeElement);
    }
    VisitableClass visitableClass = new VisitableClass();
    visitableClass.simpleName = typeElement.getSimpleName().toString();
    visitableClass.packageName = MoreElements.getPackage(typeElement).getQualifiedName().toString();
    visitableClass.fieldNames = allFieldsNames;
    visitableClass.isContext = isAnnotationPresent(typeElement, Context.class);
    if (typeElement.getSuperclass() != null
        && MoreElements.getPackage(typeElement)
            .getQualifiedName()
            .equals(
                MoreElements.getPackage(typeUtils.asElement(typeElement.getSuperclass()))
                    .getQualifiedName())) {
      visitableClass.superclassName =
          typeUtils.asElement(typeElement.getSuperclass()).getSimpleName().toString();
    }
    return visitableClass;
  }

  private boolean hasAcceptMethod(final TypeElement typeElement) {
    return FluentIterable.from(ElementFilter.methodsIn(typeElement.getEnclosedElements()))
        .anyMatch(
            new Predicate<ExecutableElement>() {
              @Override
              public boolean apply(@Nullable ExecutableElement executableElement) {
                return executableElement.getSimpleName().contentEquals("accept")
                    && executableElement.getParameters().size() == 1
                    && processingEnv
                        .getTypeUtils()
                        .asElement(executableElement.getParameters().get(0).asType())
                        .getSimpleName()
                        .contentEquals("Processor");
              }
            });
  }

  private static Predicate<VariableElement> hasAnnotation(
      final Class<? extends Annotation> annotation) {
    return new Predicate<VariableElement>() {

      @Override
      public boolean apply(@Nullable VariableElement input) {
        return isAnnotationPresent(input, annotation);
      }
    };
  }

  private void processType(TypeElement type) {
    VisitableClass visitableClass = extractVisitableClass(type);
    writeVisitableClasssHelper(visitableClass);
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

  private void writeVisitableClasssHelper(VisitableClass visitableClass) {
    boolean success = false;
    try {
      VelocityContext velocityContext = createVelocityContextForVisitorHelper(visitableClass);
      StringWriter writer = new StringWriter();

      success =
          velocityEngine.mergeTemplate(
              VISITABLE_CLASS_TEMPLATE_FILE,
              StandardCharsets.UTF_8.name(),
              velocityContext,
              writer);
      writeString(
          visitableClass.packageName + ".Visitor_" + visitableClass.simpleName, writer.toString());
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
      VelocityContext velocityContext =
          createVelocityContextForVisitor(packageName, visitableClasses);
      StringWriter writer = new StringWriter();

      success =
          velocityEngine.mergeTemplate(
              templateName, StandardCharsets.UTF_8.name(), velocityContext, writer);
      writeString(Joiner.on('.').join(packageName, className), writer.toString());
    } catch (Throwable e) {
      StringWriter stringWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stringWriter));
      reportError("Could not generate visitor class" + e + stringWriter.toString());
      success = false;
    } finally {
      if (!success) {
        reportError("Could not generate visitor class");
      }
    }
  }
}
