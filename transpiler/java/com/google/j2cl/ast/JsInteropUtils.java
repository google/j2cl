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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

/**
 * Utility functions for JsInterop properties.
 */
public class JsInteropUtils {
  private static final String JS_METHOD_ANNOTATION_NAME = "jsinterop.annotations.JsMethod";
  private static final String JS_PROPERTY_ANNOTATION_NAME = "jsinterop.annotations.JsProperty";
  private static final String JS_TYPE_ANNOTATION_NAME = "jsinterop.annotations.JsType";

  public static boolean isGlobal(String jsNamespace) {
    // TODO: a quick fix for the change in jsinterop/annotations/JsPackage.java.
    // Do a follow up cleanup to remove the check for empty string.
    return "<global>".equals(jsNamespace) || "".equals(jsNamespace);
  }

  public static IAnnotationBinding getJsTypeAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_TYPE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsMethodAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_METHOD_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsPropertyAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_PROPERTY_ANNOTATION_NAME);
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
    // check @JsProperty annotation
    IAnnotationBinding jsPropertyAnnotation = getJsPropertyAnnotation(methodBinding);
    if (jsPropertyAnnotation != null) {
      String jsPropertyName = getJsName(jsPropertyAnnotation);
      String jsPropertyNamespace = getJsNamespace(jsPropertyAnnotation);
      if (jsPropertyName != null) {
        // A JsProperty name is specified, it is a {@code PROPERTY} type.
        return JsInfo.create(JsMemberType.PROPERTY, jsPropertyName, jsPropertyNamespace);
      } else {
        // Otherwise, it is a JsProperty getter or setter.
        return JsInfo.create(
            methodBinding.getParameterTypes().length == 0
                ? JsMemberType.GETTER
                : JsMemberType.SETTER,
            jsPropertyName,
            jsPropertyNamespace);
      }
    }
    // check @JsMethod annotation
    IAnnotationBinding jsMethodAnnotation = getJsMethodAnnotation(methodBinding);
    if (jsMethodAnnotation != null) {
      String jsMethodName = getJsName(jsMethodAnnotation);
      String jsMethodNamespace = getJsNamespace(jsMethodAnnotation);
      return JsInfo.create(JsMemberType.METHOD, jsMethodName, jsMethodNamespace);
    }
    // check @JsType annotation
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(methodBinding.getDeclaringClass());
    if (jsTypeAnnotation != null && Modifier.isPublic(methodBinding.getModifiers())) {
      return JsInfo.create(JsMemberType.METHOD, null, null);
    }
    return JsInfo.NONE;
  }

  /**
   * Returns true if the method is a directly annotated as some flavor of JsMember.
   */
  public static boolean isJsMember(IMethodBinding methodBinding) {
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
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(methodBinding.getDeclaringClass());
    return jsTypeAnnotation != null && Modifier.isPublic(methodBinding.getModifiers());
  }

  public static boolean isJsProperty(IVariableBinding variableBinding) {
    Preconditions.checkArgument(variableBinding.isField());
    IAnnotationBinding jsPropertyAnnotation =
        JdtAnnotationUtils.findAnnotationBindingByName(
            variableBinding.getAnnotations(), JS_PROPERTY_ANNOTATION_NAME);
    if (jsPropertyAnnotation != null) {
      return true;
    }
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(variableBinding.getDeclaringClass());
    return jsTypeAnnotation != null && Modifier.isPublic(variableBinding.getModifiers());
  }
}
