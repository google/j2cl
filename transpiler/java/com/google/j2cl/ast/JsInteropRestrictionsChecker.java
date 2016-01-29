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
package com.google.j2cl.ast;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.j2cl.errors.Errors;

/**
 * Checks and throws errors for invalid JsInterop constructs.
 */
public class JsInteropRestrictionsChecker {
  private final Errors errors;

  public JsInteropRestrictionsChecker(Errors errors) {
    this.errors = errors;
  }

  public static void check(CompilationUnit compilationUnit, Errors errors) {
    new JsInteropRestrictionsChecker(errors).checkCompilationUnit(compilationUnit);
  }

  private void checkCompilationUnit(CompilationUnit compilationUnit) {
    for (JavaType javaType : compilationUnit.getTypes()) {
      checkJavaType(javaType);
    }
  }

  private void checkJavaType(JavaType javaType) {
    for (Field field : javaType.getFields()) {
      checkField(field);
    }
    for (Method method : javaType.getMethods()) {
      checkMethod(method);
    }
    // TODO: do other checks.
  }

  private void checkField(Field field) {
    if (field.getDescriptor().isJsOverlay()) {
      checkJsOverlay(field);
    }
    // TODO: do other checks.
  }

  private void checkMethod(Method method) {
    if (method.getDescriptor().isJsOverlay()) {
      checkJsOverlay(method);
    }
    // TODO: do other checks.
  }

  private void checkJsOverlay(Field field) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    String readableDescription = getReadableDescription(fieldDescriptor);
    if (!fieldDescriptor.getEnclosingClassTypeDescriptor().isNative()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "JsOverlay '%s' can only be declared in a native type.",
          readableDescription);
    }
    if (!fieldDescriptor.isStatic()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "JsOverlay field '%s' can only be static.",
          readableDescription);
    }
  }

  private void checkJsOverlay(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    String readableDescription = getReadableDescription(methodDescriptor);
    if (!methodDescriptor.getEnclosingClassTypeDescriptor().isNative()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "JsOverlay '%s' can only be declared in a native type.",
          readableDescription);
    }
    if (method.isOverride()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "JsOverlay method '%s' cannot override a supertype method.",
          readableDescription);
      return;
    }
    if (method.isNative() || (!method.isFinal() && !method.getDescriptor().isStatic())) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "JsOverlay method '%s' cannot be non-final nor native.",
          readableDescription);
    }
  }

  private String getReadableDescription(FieldDescriptor fieldDescriptor) {
    return String.format(
        "%s %s.%s",
        getReadableDescription(fieldDescriptor.getTypeDescriptor()),
        getReadableDescription(fieldDescriptor.getEnclosingClassTypeDescriptor()),
        fieldDescriptor.getFieldName());
  }

  private String getReadableDescription(MethodDescriptor methodDescriptor) {
    return String.format(
        "%s%s.%s(%s)",
        methodDescriptor.isConstructor()
            ? ""
            : getReadableDescription(methodDescriptor.getReturnTypeDescriptor()) + " ",
        getReadableDescription(methodDescriptor.getEnclosingClassTypeDescriptor()),
        methodDescriptor.getMethodName(),
        Joiner.on(", ")
            .join(
                Iterables.transform(
                    methodDescriptor.getParameterTypeDescriptors(),
                    new Function<TypeDescriptor, String>() {
                      @Override
                      public String apply(TypeDescriptor type) {
                        return getReadableDescription(type);
                      }
                    })));
  }

  private String getReadableDescription(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getClassName();
  }
}
