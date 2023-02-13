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
package com.google.j2cl.transpiler.frontend.javac;

import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/** Utility functions for JsInterop properties. */
public final class JsInteropUtils {
  /**
   * Simply resolve the JsInfo from annotations. Do not do any extra computations. For example, if
   * there is no "name" is specified in the annotation, just returns null for JsName.
   */
  public static JsInfo getJsInfo(ExecutableElement member) {
    AnnotationMirror annotation = JsInteropAnnotationUtils.getJsMethodAnnotation(member);
    if (annotation == null) {
      annotation = JsInteropAnnotationUtils.getJsConstructorAnnotation(member);
    }
    if (annotation == null) {
      annotation = JsInteropAnnotationUtils.getJsPropertyAnnotation(member);
    }

    boolean isPropertyAccessor = JsInteropAnnotationUtils.getJsPropertyAnnotation(member) != null;
    return getJsInfo(
        member, (TypeElement) member.getEnclosingElement(), annotation, isPropertyAccessor);
  }

  public static JsInfo getJsInfo(VariableElement member) {
    AnnotationMirror annotation = JsInteropAnnotationUtils.getJsPropertyAnnotation(member);
    return getJsInfo(member, (TypeElement) member.getEnclosingElement(), annotation, false);
  }

  private static JsInfo getJsInfo(
      Element member,
      TypeElement declaringType,
      AnnotationMirror memberAnnotation,
      boolean isAccessor) {

    boolean jsOverlay = isJsOverlay(member);
    boolean jsAsync = isJsAsync(member);

    if (JsInteropAnnotationUtils.getJsIgnoreAnnotation(member) == null) {
      boolean publicMemberOfJsType =
          isJsType(declaringType) && member.getModifiers().contains(Modifier.PUBLIC);
      boolean isJsEnumConstant =
          isJsEnum(declaringType) && member.getKind() == ElementKind.ENUM_CONSTANT;
      boolean memberOfNativeType = isJsNativeType(declaringType) && !isJsEnum(declaringType);
      if (memberAnnotation != null
          || ((publicMemberOfJsType || isJsEnumConstant || memberOfNativeType) && !jsOverlay)) {
        return JsInfo.newBuilder()
            .setJsMemberType(getJsMemberType(member, isAccessor))
            .setJsName(JsInteropAnnotationUtils.getJsName(memberAnnotation))
            .setJsNamespace(JsInteropAnnotationUtils.getJsNamespace(memberAnnotation))
            .setJsOverlay(jsOverlay)
            .setJsAsync(jsAsync)
            .setHasJsMemberAnnotation(memberAnnotation != null)
            .build();
      }
    }

    return JsInfo.newBuilder()
        .setJsMemberType(JsMemberType.NONE)
        .setJsOverlay(jsOverlay)
        .setJsAsync(jsAsync)
        .build();
  }

  @Nullable
  public static JsEnumInfo getJsEnumInfo(TypeElement type) {
    if (!isJsEnum(type)) {
      return null;
    }
    boolean hasCustomValue = JsInteropAnnotationUtils.hasCustomValue(type);
    return JsEnumInfo.newBuilder()
        .setHasCustomValue(hasCustomValue)
        .setSupportsComparable(!hasCustomValue || isJsNativeType(type))
        .setSupportsOrdinal(!hasCustomValue && !isJsNativeType(type))
        .build();
  }

  private static JsMemberType getJsMemberType(Element member, boolean isPropertyAccessor) {
    if (member.getKind() == ElementKind.FIELD || member.getKind() == ElementKind.ENUM_CONSTANT) {
      return JsMemberType.PROPERTY;
    }
    if (member.getKind() == ElementKind.CONSTRUCTOR) {
      return JsMemberType.CONSTRUCTOR;
    }
    if (isPropertyAccessor) {
      return getJsPropertyAccessorType((ExecutableElement) member);
    }
    return JsMemberType.METHOD;
  }

  private static JsMemberType getJsPropertyAccessorType(ExecutableElement method) {
    if (method.getParameters().size() == 1 && returnsPrimitiveVoid(method)) {
      return JsMemberType.SETTER;
    } else if (method.getParameters().isEmpty()
        && (!returnsPrimitiveVoid(method) || isDebugger(method))) {
      return JsMemberType.GETTER;
    }
    return JsMemberType.UNDEFINED_ACCESSOR;
  }

  private static boolean returnsPrimitiveVoid(ExecutableElement method) {
    return method.getReturnType().getKind() == TypeKind.VOID;
  }

  private static boolean isDebugger(ExecutableElement method) {
    Set<Modifier> modifiers = method.getModifiers();
    return method.getSimpleName().contentEquals("debugger")
        && modifiers.contains(Modifier.NATIVE)
        && method.getModifiers().contains(Modifier.STATIC);
  }

  /**
   * Returns true if the method is a a JsMember because of immediate conditions (either it is
   * directly annotated or it's enclosing class is annotated).
   */
  public static boolean isJsMember(ExecutableElement method) {
    return getJsInfo(method).getJsMemberType() != JsMemberType.NONE;
  }

  public static boolean isJsAsync(AnnotatedConstruct method) {
    return JsInteropAnnotationUtils.getJsAsyncAnnotation(method) != null;
  }

  public static boolean isJsOverlay(AnnotatedConstruct method) {
    return JsInteropAnnotationUtils.getJsOverlayAnnotation(method) != null;
  }

  public static boolean isJsOptional(ExecutableElement method, int i) {
    return JsInteropAnnotationUtils.getJsOptionalAnnotation(method, i) != null;
  }

  public static boolean isDoNotAutobox(ExecutableElement method, int i) {
    return JsInteropAnnotationUtils.getDoNotAutoboxAnnotation(method, i) != null;
  }

  public static boolean isJsType(AnnotatedConstruct type) {
    return JsInteropAnnotationUtils.getJsTypeAnnotation(type) != null;
  }

  public static boolean isJsEnum(AnnotatedConstruct type) {
    return JsInteropAnnotationUtils.getJsEnumAnnotation(type) != null;
  }

  public static boolean isJsNativeType(AnnotatedConstruct type) {
    return JsInteropAnnotationUtils.isJsNative(type);
  }

  public static boolean isJsFunction(AnnotatedConstruct type) {
    return JsInteropAnnotationUtils.getJsFunctionAnnotation(type) != null;
  }

  private JsInteropUtils() {}
}
