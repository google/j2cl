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
package com.google.j2cl.transpiler.frontend.jdt;

import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

/** Utility functions for JsInterop properties. */
public final class JsInteropUtils {
  /**
   * Simply resolve the JsInfo from annotations. Do not do any extra computations. For example, if
   * there is no "name" is specified in the annotation, just returns null for JsName.
   */
  public static JsInfo getJsInfo(IMethodBinding methodBinding) {
    IAnnotationBinding annotation = JsInteropAnnotationUtils.getJsMethodAnnotation(methodBinding);
    if (annotation == null) {
      annotation = JsInteropAnnotationUtils.getJsConstructorAnnotation(methodBinding);
    }
    if (annotation == null) {
      annotation = JsInteropAnnotationUtils.getJsPropertyAnnotation(methodBinding);
    }

    boolean isPropertyAccessor =
        JsInteropAnnotationUtils.getJsPropertyAnnotation(methodBinding) != null;
    return getJsInfo(
        methodBinding, methodBinding.getDeclaringClass(), annotation, isPropertyAccessor);
  }

  public static JsInfo getJsInfo(IVariableBinding field) {
    IAnnotationBinding annotation = JsInteropAnnotationUtils.getJsPropertyAnnotation(field);
    return getJsInfo(field, field.getDeclaringClass(), annotation, false);
  }

  private static JsInfo getJsInfo(
      IBinding member,
      ITypeBinding declaringType,
      IAnnotationBinding memberAnnotation,
      boolean isAccessor) {

    boolean jsOverlay = isJsOverlay(member);
    boolean jsAsync = isJsAsync(member);

    if (JsInteropAnnotationUtils.getJsIgnoreAnnotation(member) == null) {
      boolean publicMemberOfJsType =
          isJsType(declaringType) && Modifier.isPublic(member.getModifiers());
      boolean isJsEnumConstant =
          isJsEnum(declaringType)
              && member instanceof IVariableBinding
              && ((IVariableBinding) member).isEnumConstant();
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
        .setHasJsMemberAnnotation(memberAnnotation != null)
        .build();
  }

  @Nullable
  public static JsEnumInfo getJsEnumInfo(ITypeBinding typeBinding) {
    if (!isJsEnum(typeBinding)) {
      return null;
    }
    boolean hasCustomValue = JsInteropAnnotationUtils.hasCustomValue(typeBinding);
    return JsEnumInfo.newBuilder()
        .setHasCustomValue(hasCustomValue)
        .setSupportsComparable(!hasCustomValue || isJsNativeType(typeBinding))
        .setSupportsOrdinal(!hasCustomValue && !isJsNativeType(typeBinding))
        .build();
  }

  private static JsMemberType getJsMemberType(IBinding member, boolean isPropertyAccessor) {
    if (member instanceof IVariableBinding) {
      return JsMemberType.PROPERTY;
    }
    IMethodBinding method = (IMethodBinding) member;
    if (method.isConstructor()) {
      return JsMemberType.CONSTRUCTOR;
    }
    if (isPropertyAccessor) {
      return getJsPropertyAccessorType(method);
    }
    return JsMemberType.METHOD;
  }

  private static JsMemberType getJsPropertyAccessorType(IMethodBinding methodBinding) {
    if (methodBinding.getParameterTypes().length == 1 && returnsPrimitiveVoid(methodBinding)) {
      return JsMemberType.SETTER;
    } else if (methodBinding.getParameterTypes().length == 0
        && (!returnsPrimitiveVoid(methodBinding) || isDebugger(methodBinding))) {
      return JsMemberType.GETTER;
    }
    return JsMemberType.UNDEFINED_ACCESSOR;
  }

  private static boolean returnsPrimitiveVoid(IMethodBinding methodBinding) {
    return methodBinding
        .getReturnType()
        .getQualifiedName()
        .equals(PrimitiveTypes.VOID.getSimpleSourceName());
  }

  private static boolean isDebugger(IMethodBinding methodBinding) {
    int modifiers = methodBinding.getModifiers();
    return methodBinding.getName().equals("debugger")
        && Modifier.isNative(modifiers)
        && JdtEnvironment.isStatic(methodBinding);
  }

  public static boolean isJsAsync(IBinding methodBinding) {
    return JsInteropAnnotationUtils.getJsAsyncAnnotation(methodBinding) != null;
  }

  public static boolean isJsOverlay(IBinding methodBinding) {
    return JsInteropAnnotationUtils.getJsOverlayAnnotation(methodBinding) != null;
  }

  public static boolean isJsOptional(IMethodBinding methodBinding, int i) {
    return JsInteropAnnotationUtils.getJsOptionalAnnotation(methodBinding, i) != null;
  }

  public static boolean isDoNotAutobox(IMethodBinding methodBinding, int i) {
    return JsInteropAnnotationUtils.getDoNotAutoboxAnnotation(methodBinding, i) != null;
  }

  public static boolean isJsType(ITypeBinding typeBinding) {
    return JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding) != null;
  }

  public static boolean isJsEnum(ITypeBinding typeBinding) {
    return JsInteropAnnotationUtils.getJsEnumAnnotation(typeBinding) != null;
  }

  public static boolean isJsNativeType(ITypeBinding declaringType) {
    return JsInteropAnnotationUtils.isJsNative(declaringType);
  }

  public static boolean isJsFunction(ITypeBinding typeBinding) {
    return JsInteropAnnotationUtils.getJsFunctionAnnotation(typeBinding) != null;
  }

  private JsInteropUtils() {}
}
