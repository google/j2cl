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
  private static final String JS_TYPE_ANNOTATION_NAME = "jsinterop.annotations.JsType";

  public static IAnnotationBinding getJsTypeAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_TYPE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsMethodAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_METHOD_ANNOTATION_NAME);
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

  public static boolean isJsMethod(IMethodBinding methodBinding) {
    IAnnotationBinding jsMethodAnnotation = getJsMethodAnnotation(methodBinding);
    if (jsMethodAnnotation != null) {
      return true;
    }
    // public methods in JsType annotated class are JsMethod.
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(methodBinding.getDeclaringClass());
    return jsTypeAnnotation != null && Modifier.isPublic(methodBinding.getModifiers());
  }

  public static boolean isJsProperty(IVariableBinding variableBinding) {
    Preconditions.checkArgument(variableBinding.isField());
    // TODO: add @JsProperty check.
    // public fields in JsType annotated class are JsProperty.
    IAnnotationBinding jsTypeAnnotation = getJsTypeAnnotation(variableBinding.getDeclaringClass());
    return jsTypeAnnotation != null && Modifier.isPublic(variableBinding.getModifiers());
  }
}
