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
  public static boolean isJsFunction(ITypeBinding typeBinding) {
    return JsInteropAnnotationUtils.getJsFunctionAnnotation(typeBinding) != null;
  }

  /**
   * Simply resolve the JsInfo from annotations. Do not do any extra computations. For example, if
   * there is no "name" is specified in the annotation, just returns null for JsName.
   */
  public static JsInfo getJsInfo(IMethodBinding methodBinding) {
    // check @JsIgnore annotation
    IAnnotationBinding jsIgnoreAnnotation =
        JsInteropAnnotationUtils.getJsIgnoreAnnotation(methodBinding);
    if (jsIgnoreAnnotation != null) {
      return JsInfo.NONE;
    }
    boolean isJsOverlay = isJsOverlay(methodBinding);
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation =
        JsInteropAnnotationUtils.getJsPropertyAnnotation(methodBinding);
    if (jsPropertyAnnotation != null) {
      String jsPropertyName = JsInteropAnnotationUtils.getJsName(jsPropertyAnnotation);
      String jsPropertyNamespace = JsInteropAnnotationUtils.getJsNamespace(jsPropertyAnnotation);
      if (jsPropertyName != null) {
        // A JsProperty name is specified, it is a {@code PROPERTY} type.
        return JsInfo.create(
            JsMemberType.PROPERTY, jsPropertyName, jsPropertyNamespace, isJsOverlay);
      } else {
        // Otherwise, it is a JsProperty getter or setter.
        return JsInfo.create(
            methodBinding.getParameterTypes().length == 0
                ? JsMemberType.GETTER
                : JsMemberType.SETTER,
            jsPropertyName,
            jsPropertyNamespace,
            isJsOverlay);
      }
    }
    // check @JsMethod annotation
    IAnnotationBinding jsMethodAnnotation =
        JsInteropAnnotationUtils.getJsMethodAnnotation(methodBinding);
    if (jsMethodAnnotation != null) {
      String jsMethodName = JsInteropAnnotationUtils.getJsName(jsMethodAnnotation);
      String jsMethodNamespace = JsInteropAnnotationUtils.getJsNamespace(jsMethodAnnotation);
      return JsInfo.create(JsMemberType.METHOD, jsMethodName, jsMethodNamespace, isJsOverlay);
    }
    // check @JsConstructor annotation
    IAnnotationBinding jsConstructorAnnotation =
        JsInteropAnnotationUtils.getJsConstructorAnnotation(methodBinding);
    if (jsConstructorAnnotation != null) {
      String jsName = JsInteropAnnotationUtils.getJsName(jsConstructorAnnotation);
      String jsNamespace = JsInteropAnnotationUtils.getJsNamespace(jsConstructorAnnotation);
      return JsInfo.create(JsMemberType.CONSTRUCTOR, jsName, jsNamespace, isJsOverlay);
    }
    // check @JsType annotation
    IAnnotationBinding jsTypeAnnotation =
        JsInteropAnnotationUtils.getJsTypeAnnotation(methodBinding.getDeclaringClass());
    // In native @JsType all members (regardless of visibility) is implicit JsProperty/JsMethod.
    if (jsTypeAnnotation != null
        && !isJsOverlay
        && (Modifier.isPublic(methodBinding.getModifiers())
            || JsInteropAnnotationUtils.isNative(jsTypeAnnotation))) {
      return JsInfo.create(
          methodBinding.isConstructor() ? JsMemberType.CONSTRUCTOR : JsMemberType.METHOD,
          null,
          null,
          false);
    }
    // check @JsFunction annotation
    IAnnotationBinding jsFunctionAnnotation =
        JsInteropAnnotationUtils.getJsFunctionAnnotation(methodBinding.getDeclaringClass());
    if (jsFunctionAnnotation != null) {
      return JsInfo.create(JsMemberType.JS_FUNCTION, null, null, isJsOverlay);
    }
    return JsInfo.create(JsMemberType.NONE, null, null, isJsOverlay);
  }

  /**
   * Simply resolve the JsInfo from annotations. Do not do any extra computations. For example, if
   * there is no "name" is specified in the annotation, just returns null for JsName.
   */
  public static JsInfo getJsInfo(IVariableBinding variableBinding) {
    // check @JsIgnore annotation
    IAnnotationBinding jsIgnoreAnnotation =
        JsInteropAnnotationUtils.getJsIgnoreAnnotation(variableBinding);
    if (jsIgnoreAnnotation != null) {
      return JsInfo.NONE;
    }
    boolean isJsOverlay = isJsOverlay(variableBinding);
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation =
        JsInteropAnnotationUtils.getJsPropertyAnnotation(variableBinding);
    if (jsPropertyAnnotation != null) {
      String jsPropertyName = JsInteropAnnotationUtils.getJsName(jsPropertyAnnotation);
      String jsPropertyNamespace = JsInteropAnnotationUtils.getJsNamespace(jsPropertyAnnotation);
      return JsInfo.create(JsMemberType.PROPERTY, jsPropertyName, jsPropertyNamespace, isJsOverlay);
    }
    // check @JsType annotation
    IAnnotationBinding jsTypeAnnotation =
        JsInteropAnnotationUtils.getJsTypeAnnotation(variableBinding.getDeclaringClass());
    // In native @JsType all members (regardless of visibility) is implicit JsProperty/JsMethod.
    if (jsTypeAnnotation != null
        && (Modifier.isPublic(variableBinding.getModifiers())
            || JsInteropAnnotationUtils.isNative(jsTypeAnnotation))) {
      return JsInfo.create(JsMemberType.PROPERTY, null, null, isJsOverlay);
    }
    return JsInfo.create(JsMemberType.NONE, null, null, isJsOverlay);
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
}
