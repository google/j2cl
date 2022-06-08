/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.DO_NOT_AUTOBOX_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ASYNC_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_CONSTRUCTOR_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ENUM_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_FUNCTION_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_IGNORE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_METHOD_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OVERLAY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PACKAGE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_TYPE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.SUPPRESS_WARNINGS_ANNOTATION_NAME;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.collect.Streams;
import com.google.j2cl.common.InternalCompilerError;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/** Utility methods to get information about Js Interop annotations. */
public class JsInteropAnnotationUtils {

  private JsInteropAnnotationUtils() {}

  public static AnnotationMirror getJsAsyncAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_ASYNC_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsConstructorAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_CONSTRUCTOR_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsEnumAnnotation(AnnotatedConstruct typeBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotationMirrors(), JS_ENUM_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsFunctionAnnotation(AnnotatedConstruct typeBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotationMirrors(), JS_FUNCTION_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsIgnoreAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_IGNORE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsTypeAnnotation(AnnotatedConstruct typeBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotationMirrors(), JS_TYPE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsMethodAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_METHOD_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsPackageAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_PACKAGE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsPropertyAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_PROPERTY_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsOptionalAnnotation(
      ExecutableElement methodBinding, int parameterIndex) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getParameters().get(parameterIndex).getAnnotationMirrors(),
        JS_OPTIONAL_ANNOTATION_NAME);
  }

  public static AnnotationMirror getDoNotAutoboxAnnotation(
      ExecutableElement methodBinding, int parameterIndex) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getParameters().get(parameterIndex).getAnnotationMirrors(),
        DO_NOT_AUTOBOX_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsOverlayAnnotation(AnnotatedConstruct methodBinding) {
    return AnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotationMirrors(), JS_OVERLAY_ANNOTATION_NAME);
  }

  /**
   * Helper method to recover annotations through looking at the internal representation.
   *
   * <p>TODO(b/136507005): Javac does not keep annotations accessible for types outside compilation.
   * boundaries.
   */
  public static Stream<Compound> getDeclarationAndTypeAttributes(Symbol sym) {
    Symbol typeAnnotationOwner;
    switch (sym.getKind()) {
      case PARAMETER:
        // If sym is a parameter, the annotations are stored in the "owner", i.e. the method.
        typeAnnotationOwner = sym.owner;
        break;
      default:
        typeAnnotationOwner = sym;
    }
    // Even though javac does not load annotations for types/elements accross a compilation
    // boundaries, a list of all the annotations related to the type/element are stored. These
    // annotations are stored with a position mark to know whether, for example, they apply to
    // a parameter, etc. We filter out all the annotations that are NOT related to the sym.
    return Streams.concat(
            sym.getRawAttributes().stream(),
            typeAnnotationOwner.getRawTypeAttributes().stream()
                .filter(anno -> isAnnotationOnType(sym, anno.position)))
        // Dedup annotations, since the annotation might be returned multiple times (but
        // represented as different instances) just group them by qualified name and return the
        // first one of each.
        .collect(
            groupingBy(
                c -> c.type.asElement().getQualifiedName(), LinkedHashMap::new, toImmutableList()))
        .values()
        .stream()
        .map(c -> c.get(0));
  }

  /** Determines whether the annotation is an annotation on the symbol {@code sym}. */
  private static boolean isAnnotationOnType(Symbol sym, TypeAnnotationPosition position) {
    if (!position.location.isEmpty()) {
      return false;
    }
    switch (sym.getKind()) {
      case LOCAL_VARIABLE:
        return position.type == TargetType.LOCAL_VARIABLE;
      case FIELD:
        return position.type == TargetType.FIELD;
      case CONSTRUCTOR:
      case METHOD:
        return position.type == TargetType.METHOD_RETURN;
      case PARAMETER:
        switch (position.type) {
          case METHOD_FORMAL_PARAMETER:
            return ((MethodSymbol) sym.owner).getParameters().indexOf(sym)
                == position.parameter_index;
          default:
            return false;
        }
      case CLASS:
        // There are no type annotations on the top-level type of the class being declared, only
        // on other types in the signature (e.g. `class Foo extends Bar<@A Baz> {}`).
        return false;
      default:
        throw new InternalCompilerError(
            "Unsupported element kind in MoreAnnotation#isAnnotationOnType: %s.", sym.getKind());
    }
  }

  public static boolean isJsPackageAnnotation(AnnotationMirror annotation) {
    return ((TypeElement) annotation.getAnnotationType().asElement())
        .getQualifiedName()
        .contentEquals(JS_PACKAGE_ANNOTATION_NAME);
  }

  public static boolean isJsNative(AnnotatedConstruct typeBinding) {
    return isJsNative(getJsTypeOrJsEnumAnnotation(typeBinding));
  }

  private static boolean isJsNative(AnnotationMirror annotationBinding) {
    return AnnotationUtils.getAnnotationParameterBoolean(annotationBinding, "isNative", false);
  }

  public static boolean isUnusableByJsSuppressed(AnnotatedConstruct binding) {
    AnnotationMirror suppressWarningsBinding =
        AnnotationUtils.findAnnotationBindingByName(
            binding.getAnnotationMirrors(), SUPPRESS_WARNINGS_ANNOTATION_NAME);
    if (suppressWarningsBinding == null) {
      return false;
    }
    List<?> suppressions =
        AnnotationUtils.getAnnotationParameterArray(suppressWarningsBinding, "value");
    return suppressions.stream().map(Object::toString).anyMatch("\"unusable-by-js\""::equals);
  }

  /** The namespace specified on a package, type, method or field. */
  public static String getJsNamespace(AnnotatedConstruct typeBinding) {
    AnnotationMirror annotation = getJsTypeAnnotation(typeBinding);
    if (annotation == null) {
      annotation = getJsEnumAnnotation(typeBinding);
    }
    if (annotation == null) {
      annotation = getJsPackageAnnotation(typeBinding);
    }
    return getJsNamespace(annotation);
  }

  public static String getJsNamespace(AnnotationMirror annotationBinding) {
    return AnnotationUtils.getAnnotationParameterString(annotationBinding, "namespace");
  }

  public static String getJsName(AnnotatedConstruct typeBinding) {
    return getJsName(getJsTypeOrJsEnumAnnotation(typeBinding));
  }

  public static String getJsName(AnnotationMirror annotationBinding) {
    return AnnotationUtils.getAnnotationParameterString(annotationBinding, "name");
  }

  private static AnnotationMirror getJsTypeOrJsEnumAnnotation(AnnotatedConstruct typeBinding) {
    return Optional.ofNullable(getJsTypeAnnotation(typeBinding))
        .orElse(getJsEnumAnnotation(typeBinding));
  }

  public static boolean hasCustomValue(AnnotatedConstruct typeBinding) {
    return hasCustomValue(getJsEnumAnnotation(typeBinding));
  }

  private static boolean hasCustomValue(AnnotationMirror annotationBinding) {
    return AnnotationUtils.getAnnotationParameterBoolean(
        annotationBinding, "hasCustomValue", false);
  }
}
