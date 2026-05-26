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

import static com.google.j2cl.transpiler.frontend.javac.JavaEnvironment.getEnclosingTypeElement;
import static com.google.j2cl.transpiler.frontend.javac.JavaEnvironment.isRecord;
import static com.google.j2cl.transpiler.frontend.javac.JavaEnvironment.isStatic;

import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
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
      // Do not read annotations for synthetic accessor (which are copied from the record
      // components). We will continue reading annotations from custom accessors so that they
      // can be checked and rejected by the restriction checker.
      boolean generated = (((Symbol) member).flags() & Flags.GENERATED_MEMBER) != 0;
      if (!generated) {
        annotation = JsInteropAnnotationUtils.getJsPropertyAnnotation(member);
      }
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
      boolean implicitJsMember = isJsType(declaringType) && canBeImplicitJsMember(member);
      boolean isJsEnumConstant =
          isJsEnum(declaringType) && member.getKind() == ElementKind.ENUM_CONSTANT;
      boolean memberOfNativeType = isJsNativeType(declaringType) && !isJsEnum(declaringType);
      if (memberAnnotation != null
          || ((implicitJsMember || isJsEnumConstant || memberOfNativeType) && !jsOverlay)) {
        return JsInfo.builder()
            .setJsMemberType(getJsMemberType(member, isAccessor))
            .setJsName(JsInteropAnnotationUtils.getJsName(memberAnnotation))
            .setJsNamespace(JsInteropAnnotationUtils.getJsNamespace(memberAnnotation))
            .setJsOverlay(jsOverlay)
            .setJsAsync(jsAsync)
            .setHasJsMemberAnnotation(memberAnnotation != null)
            .build();
      }
    }

    return JsInfo.builder()
        .setJsMemberType(JsMemberType.NONE)
        .setJsOverlay(jsOverlay)
        .setJsAsync(jsAsync)
        .build();
  }

  private static boolean canBeImplicitJsMember(Element member) {
    // Public members are implicitly JsMembers.
    if (member.getModifiers().contains(Modifier.PUBLIC)) {
      // Component accessors will inherit JsInfo from the components so should not be considered
      // implicit JsMembers even though they are public.
      if (isRecordComponentAccessor(member)) {
        return false;
      }
      return true;
    }
    // Record components fields, although private, are implicit js members. Later in the process, it
    // will be used by public accessors to inherit JsInfo from.
    if (isRecordComponentField(member)) {
      return true;
    }
    return false;
  }

  private static boolean isRecordComponentAccessor(Element member) {
    return member.getKind() == ElementKind.METHOD
        && ((TypeElement) member.getEnclosingElement())
            .getRecordComponents().stream()
                .anyMatch(component -> component.getAccessor().equals(member));
  }

  private static boolean isRecordComponentField(Element member) {
    return member.getKind() == ElementKind.FIELD
        && !isStatic(member)
        && isRecord(getEnclosingTypeElement(member));
  }

  @Nullable
  public static JsEnumInfo getJsEnumInfo(AnnotatedConstruct annotatedConstruct) {
    if (!isJsEnum(annotatedConstruct)) {
      return null;
    }
    boolean hasCustomValue = JsInteropAnnotationUtils.hasCustomValue(annotatedConstruct);
    return JsEnumInfo.builder()
        .setHasCustomValue(hasCustomValue)
        .setSupportsComparable(!hasCustomValue || isJsNativeType(annotatedConstruct))
        .setSupportsOrdinal(!hasCustomValue && !isJsNativeType(annotatedConstruct))
        .build();
  }

  private static JsMemberType getJsMemberType(Element member, boolean isPropertyAccessor) {
    if (member.getKind().isField()) {
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

  public static boolean isJsAsync(AnnotatedConstruct annotatedConstruct) {
    return JsInteropAnnotationUtils.getJsAsyncAnnotation(annotatedConstruct) != null;
  }

  public static boolean isJsOverlay(AnnotatedConstruct annotatedConstruct) {
    return JsInteropAnnotationUtils.getJsOverlayAnnotation(annotatedConstruct) != null;
  }

  public static boolean isJsOptional(ExecutableElement method, int i) {
    return JsInteropAnnotationUtils.getJsOptionalAnnotation(method, i) != null;
  }

  public static boolean isJsType(AnnotatedConstruct annotatedConstruct) {
    return JsInteropAnnotationUtils.getJsTypeAnnotation(annotatedConstruct) != null;
  }

  public static boolean isJsEnum(AnnotatedConstruct annotatedConstruct) {
    return JsInteropAnnotationUtils.getJsEnumAnnotation(annotatedConstruct) != null;
  }

  public static boolean isJsNativeType(AnnotatedConstruct annotatedConstruct) {
    return JsInteropAnnotationUtils.isJsNative(annotatedConstruct);
  }

  public static boolean isJsFunction(AnnotatedConstruct annotatedConstruct) {
    return JsInteropAnnotationUtils.getJsFunctionAnnotation(annotatedConstruct) != null;
  }

  private JsInteropUtils() {}
}
