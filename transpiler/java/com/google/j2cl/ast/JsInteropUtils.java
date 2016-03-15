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
package com.google.j2cl.ast;

import com.google.common.base.Preconditions;
import com.google.j2cl.common.JdtAnnotationUtils;

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
  private static final String JS_CONSTRUCTOR_ANNOTATION_NAME =
      "jsinterop.annotations.JsConstructor";
  private static final String JS_FUNCTION_ANNOTATION_NAME = "jsinterop.annotations.JsFunction";
  private static final String JS_IGNORE_ANNOTATION_NAME = "jsinterop.annotations.JsIgnore";
  private static final String JS_METHOD_ANNOTATION_NAME = "jsinterop.annotations.JsMethod";
  private static final String JS_OVERLAY_ANNOTATION_NAME = "jsinterop.annotations.JsOverlay";
  private static final String JS_PROPERTY_ANNOTATION_NAME = "jsinterop.annotations.JsProperty";
  private static final String JS_TYPE_ANNOTATION_NAME = "jsinterop.annotations.JsType";
  public static final String JS_GLOBAL = "<global>";

  public static boolean isGlobal(String jsNamespace) {
    return JS_GLOBAL.equals(jsNamespace);
  }

  public static IAnnotationBinding getJsConstructorAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_CONSTRUCTOR_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsFunctionAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_FUNCTION_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsIgnoreAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_IGNORE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsTypeAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_TYPE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsMethodAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_METHOD_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsPropertyAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_PROPERTY_ANNOTATION_NAME);
  }

  public static boolean isJsFunction(ITypeBinding typeBinding) {
    return getJsFunctionAnnotation(typeBinding) != null;
  }

  public static boolean isNative(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getAnnotationParameterBoolean(annotationBinding, "isNative", false);
  }

  public static String getJsNamespace(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getAnnotationParameterString(annotationBinding, "namespace");
  }

  public static String getJsName(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getAnnotationParameterString(annotationBinding, "name");
  }

  /**
   * Simply resolve the JsInfo from annotations. Do not do any extra computations. For example, if
   * there is no "name" is specified in the annotation, just returns null for JsName.
   */
  public static JsInfo getJsInfo(IMethodBinding methodBinding) {
    // check @JsIgnore annotation
    IAnnotationBinding jsIgnoreAnnotation = getJsIgnoreAnnotation(methodBinding);
    if (jsIgnoreAnnotation != null) {
      return JsInfo.NONE;
    }
    boolean isJsOverlay = isJsOverlay(methodBinding);
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation = getJsPropertyAnnotation(methodBinding);
    if (jsPropertyAnnotation != null) {
      String jsPropertyName = getJsName(jsPropertyAnnotation);
      String jsPropertyNamespace = getJsNamespace(jsPropertyAnnotation);
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
    IAnnotationBinding jsMethodAnnotation = getJsMethodAnnotation(methodBinding);
    if (jsMethodAnnotation != null) {
      String jsMethodName = getJsName(jsMethodAnnotation);
      String jsMethodNamespace = getJsNamespace(jsMethodAnnotation);
      return JsInfo.create(JsMemberType.METHOD, jsMethodName, jsMethodNamespace, isJsOverlay);
    }
    // check @JsConstructor annotation
    IAnnotationBinding jsConstructorAnnotation = getJsConstructorAnnotation(methodBinding);
    if (jsConstructorAnnotation != null) {
      String jsName = getJsName(jsConstructorAnnotation);
      String jsNamespace = getJsNamespace(jsConstructorAnnotation);
      return JsInfo.create(JsMemberType.CONSTRUCTOR, jsName, jsNamespace, isJsOverlay);
    }
    // check @JsType annotation
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(methodBinding.getDeclaringClass());
    // In native @JsType all members (regardless of visibility) is implicit JsProperty/JsMethod.
    if (jsTypeAnnotation != null
        && !isJsOverlay
        && (Modifier.isPublic(methodBinding.getModifiers())
            || JsInteropUtils.isNative(jsTypeAnnotation))) {
      return JsInfo.create(
          methodBinding.isConstructor() ? JsMemberType.CONSTRUCTOR : JsMemberType.METHOD,
          null,
          null,
          isJsOverlay);
    }
    // check @JsFunction annotation
    IAnnotationBinding jsFunctionAnnotation =
        getJsFunctionAnnotation(methodBinding.getDeclaringClass());
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
    IAnnotationBinding jsIgnoreAnnotation = getJsIgnoreAnnotation(variableBinding);
    if (jsIgnoreAnnotation != null) {
      return JsInfo.NONE;
    }
    boolean isJsOverlay = isJsOverlay(variableBinding);
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation = getJsPropertyAnnotation(variableBinding);
    if (jsPropertyAnnotation != null) {
      String jsPropertyName = getJsName(jsPropertyAnnotation);
      String jsPropertyNamespace = getJsNamespace(jsPropertyAnnotation);
      return JsInfo.create(JsMemberType.PROPERTY, jsPropertyName, jsPropertyNamespace, isJsOverlay);
    }
    // check @JsType annotation
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(variableBinding.getDeclaringClass());
    // In native @JsType all members (regardless of visibility) is implicit JsProperty/JsMethod.
    if (jsTypeAnnotation != null
        && (Modifier.isPublic(variableBinding.getModifiers())
            || JsInteropUtils.isNative(jsTypeAnnotation))) {
      return JsInfo.create(JsMemberType.PROPERTY, null, null, isJsOverlay);
    }
    return JsInfo.create(JsMemberType.NONE, null, null, isJsOverlay);
  }

  /**
   * Returns true if the method is a a JsMember because of immediate conditions (either it is
   * directly annotated or it's enclosing class is annotated).
   */
  public static boolean isJsMember(IMethodBinding methodBinding) {
    // check @JsIgnore annotation
    IAnnotationBinding jsIgnoreAnnotation = getJsIgnoreAnnotation(methodBinding);
    if (jsIgnoreAnnotation != null) {
      return false;
    }
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation = getJsPropertyAnnotation(methodBinding);
    if (jsPropertyAnnotation != null) {
      return true;
    }
    // check @JsMethod annotation
    IAnnotationBinding jsMethodAnnotation = getJsMethodAnnotation(methodBinding);
    if (jsMethodAnnotation != null) {
      return true;
    }
    // check @JsType annotation.
    // In native @JsType all members (regardless of visibility) is implicit JsProperty/JsMethod.
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(methodBinding.getDeclaringClass());
    return jsTypeAnnotation != null
        && (Modifier.isPublic(methodBinding.getModifiers())
            || JsInteropUtils.isNative(jsTypeAnnotation));
  }

  public static boolean isJsProperty(IVariableBinding variableBinding) {
    Preconditions.checkArgument(variableBinding.isField());
    // check @JsIgnore annotation
    IAnnotationBinding jsIgnoreAnnotation = getJsIgnoreAnnotation(variableBinding);
    if (jsIgnoreAnnotation != null) {
      return false;
    }
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation = getJsPropertyAnnotation(variableBinding);
    if (jsPropertyAnnotation != null) {
      return true;
    }
    // check @JsType annotation.
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(variableBinding.getDeclaringClass());
    // In native @JsType all members (regardless of visibility) is implicit JsProperty/JsMethod.
    return jsTypeAnnotation != null
        && (Modifier.isPublic(variableBinding.getModifiers())
            || JsInteropUtils.isNative(jsTypeAnnotation));
  }

  public static boolean isJsOverlay(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
            methodBinding.getAnnotations(), JS_OVERLAY_ANNOTATION_NAME)
        != null;
  }

  public static boolean isJsConstructor(IMethodBinding methodBinding) {
    // check @JsIgnore annotation
    IAnnotationBinding jsIgnoreAnnotation = getJsIgnoreAnnotation(methodBinding);
    if (jsIgnoreAnnotation != null) {
      return false;
    }
    // check @JsConstructor annotation
    IAnnotationBinding jsConstructorAnnotation = getJsConstructorAnnotation(methodBinding);
    if (jsConstructorAnnotation != null) {
      return true;
    }
    // check @JsType annotation.
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(methodBinding.getDeclaringClass());
    // In native @JsType all constructors (regardless of visibility) is implicit JsConstructor.
    return jsTypeAnnotation != null
        && methodBinding.isConstructor()
        && (Modifier.isPublic(methodBinding.getModifiers())
            || JsInteropUtils.isNative(jsTypeAnnotation));
  }

  public static boolean isJsType(ITypeBinding typeBinding) {
    return getJsTypeAnnotation(typeBinding) != null;
  }
}
