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
package com.google.j2cl.frontend;

import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
import com.google.j2cl.ast.TypeDescriptors;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

/**
 * Utility functions for JsInterop properties.
 */
public class JsInteropUtils {
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

    if (JsInteropAnnotationUtils.getJsIgnoreAnnotation(member) == null) {
      boolean publicMemberOfJsType =
          isJsType(declaringType) && Modifier.isPublic(member.getModifiers());
      boolean memberOfNativeType = isNativeType(declaringType);
      if (memberAnnotation != null
          || ((publicMemberOfJsType || memberOfNativeType) && !jsOverlay)) {
        return JsInfo.newBuilder()
            .setJsMemberType(getJsMemberType(member, isAccessor))
            .setJsName(JsInteropAnnotationUtils.getJsName(memberAnnotation))
            .setJsNamespace(JsInteropAnnotationUtils.getJsNamespace(memberAnnotation))
            .setJsOverlay(jsOverlay)
            .build();
      }
    }

    return JsInfo.newBuilder().setJsMemberType(JsMemberType.NONE).setJsOverlay(jsOverlay).build();
  }

  public static boolean isOrOverridesJsFunctionMethod(IMethodBinding methodBinding) {
    ITypeBinding declaringType = methodBinding.getDeclaringClass();
    if (isJsFunction(declaringType)
        && declaringType.getFunctionalInterfaceMethod() != null
        && methodBinding.getMethodDeclaration()
            == declaringType.getFunctionalInterfaceMethod().getMethodDeclaration()) {
      return true;
    }
    for (IMethodBinding overriddenMethodBinding : JdtUtils.getOverriddenMethods(methodBinding)) {
      if (isOrOverridesJsFunctionMethod(overriddenMethodBinding)) {
        return true;
      }
    }
    return false;
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

  private static JsMemberType getJsPropertyAccessorType(IMethodBinding method) {
    if (method.getParameterTypes().length == 1
        && method.getReturnType().getQualifiedName().equals(TypeDescriptors.VOID_TYPE_NAME)) {
      return JsMemberType.SETTER;
    } else if (method.getParameterTypes().length == 0
    // TODO(B/35881307): void should not be allowed as a return type for property getters.
    // && !method.getReturnType().getQualifiedName().equals(TypeDescriptors.VOID_TYPE_NAME)
    ) {
      return JsMemberType.GETTER;
    }
    return JsMemberType.UNDEFINED_ACCESSOR;
  }

  /**
   * Returns true if the method is a a JsMember because of immediate conditions (either it is
   * directly annotated or it's enclosing class is annotated).
   */
  public static boolean isJsMember(IMethodBinding methodBinding) {
    return getJsInfo(methodBinding).getJsMemberType() != JsMemberType.NONE;
  }

  public static boolean isJsOverlay(IBinding methodBinding) {
    return JsInteropAnnotationUtils.getJsOverlayAnnotation(methodBinding) != null;
  }

  public static boolean isJsOptional(IMethodBinding methodBinding, int i) {
    return JsInteropAnnotationUtils.getJsOptionalAnnotation(methodBinding, i) != null;
  }

  public static boolean isJsType(ITypeBinding typeBinding) {
    return JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding) != null;
  }

  public static boolean isNativeType(ITypeBinding declaringType) {
    return JsInteropAnnotationUtils.isNative(
        JsInteropAnnotationUtils.getJsTypeAnnotation(declaringType));
  }

  public static boolean isJsFunction(ITypeBinding typeBinding) {
    return JsInteropAnnotationUtils.getJsFunctionAnnotation(typeBinding) != null;
  }
}
