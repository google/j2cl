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

import java.util.List;

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
    if (javaType.getDescriptor().isNative()) {
      if (!checkNativeJsType(javaType)) {
        return;
      }
    }
    for (Field field : javaType.getFields()) {
      checkField(field);
    }
    for (Method method : javaType.getMethods()) {
      checkMethod(method);
    }
    // TODO: do other checks.
  }

  private void checkField(Field field) {
    if (field.getDescriptor().getEnclosingClassTypeDescriptor().isNative()) {
      checkFieldOfNativeJsType(field);
    }
    if (field.getDescriptor().isJsOverlay()) {
      checkJsOverlay(field);
    }
    // TODO: do other checks.
  }

  private void checkMethod(Method method) {
    if (method.getDescriptor().getEnclosingClassTypeDescriptor().isNative()) {
      checkMethodOfNativeJsType(method);
    }
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

  private boolean checkNativeJsType(JavaType javaType) {
    TypeDescriptor typeDescriptor = javaType.getDescriptor();
    String readableDescription = getReadableDescription(typeDescriptor);

    if (typeDescriptor.isEnumOrSubclass()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "Enum '%s' cannot be a native JsType.",
          readableDescription);
      return false;
    }
    if (typeDescriptor.isInstanceNestedClass()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "Non static inner class '%s' cannot be a native JsType.",
          readableDescription);
      return false;
    }

    TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
    if (superTypeDescriptor != null
        && superTypeDescriptor != TypeDescriptors.get().javaLangObject
        && !superTypeDescriptor.isNative()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "Native JsType '%s' can only extend native JsType classes.",
          readableDescription);
    }
    for (TypeDescriptor interfaceType : javaType.getSuperInterfaceTypeDescriptors()) {
      if (!interfaceType.isNative()) {
        errors.error(
            Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
            "Native JsType '%s' can only %s native JsType interfaces.",
            readableDescription,
            javaType.isInterface() ? "extend" : "implement");
      }
    }

    if (!javaType.getInstanceInitializerBlocks().isEmpty()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "Native JsType '%s' cannot have initializer.",
          readableDescription);
    }

    if (!javaType.getStaticInitializerBlocks().isEmpty()) {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "Native JsType '%s' cannot have static initializer.",
          readableDescription);
    }
    return true;
  }

  private void checkMethodOfNativeJsType(Method method) {
    if (method.getDescriptor().isJsOverlay()) {
      return;
    }
    String readableDescription = getReadableDescription(method.getDescriptor());
    JsMemberType jsMemberType = method.getDescriptor().getJsInfo().getJsMemberType();
    switch (jsMemberType) {
      case CONSTRUCTOR:
        if (!isConstructorEmpty(method)) {
          errors.error(
              Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
              "Native JsType constructor '%s' cannot have non-empty method body.",
              readableDescription);
        }
        break;
      case METHOD:
      case GETTER:
      case SETTER:
      case PROPERTY:
        if (!method.isAbstract() && !method.isNative()) {
          errors.error(
              Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
              "Native JsType method '%s' should be native or abstract.",
              readableDescription);
        }
        break;
      case NONE:
        errors.error(
            Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
            "Native JsType member '%s' cannot have @JsIgnore.",
            readableDescription);
        break;
      default:
        break;
    }
  }

  private void checkFieldOfNativeJsType(Field field) {
    if (field.getDescriptor().isJsOverlay()) {
      return;
    }
    String readableDescription = getReadableDescription(field.getDescriptor());
    if (field.getDescriptor().isJsProperty()) {
      if (field.hasInitializer()) {
        errors.error(
            Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
            "Native JsType field '%s' cannot have initializer.",
            readableDescription);
      }
    } else {
      errors.error(
          Errors.Error.ERR_JSINTEROP_RESTRICTIONS_ERROR,
          "Native JsType member '%s' cannot have @JsIgnore.",
          readableDescription);
    }
  }

  /**
   * Returns true if the constructor method is locally empty (allows calls to super constructor).
   */
  private static boolean isConstructorEmpty(Method constructor) {
    List<Statement> statements = constructor.getBody().getStatements();
    return statements.isEmpty() || (statements.size() == 1 && AstUtils.hasSuperCall(constructor));
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
