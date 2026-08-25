/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;

/**
 * Normalizes concrete implementations of JsFunction interfaces by adapting them to extend the
 * common JsFunctionAdaptor type.
 */
public class AddJsFunctionCommonAdaptorSuperType extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (!type.isJsFunctionImplementation()) {
      return;
    }

    DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
        type.getTypeDescriptor().getFunctionalInterface();
    checkState(functionalInterfaceTypeDescriptor.isJsFunctionInterface());

    // Inject the JsFunctionAdaptor supertype for types who implement a JsFunction
    // interface.
    type.setSuperTypeDescriptor(TypeDescriptors.get().javaemulInternalJsFunctionAdaptor);

    // Implement necessary JsFunction adapter members.
    implementMembers(type, functionalInterfaceTypeDescriptor);
  }

  private static void implementMembers(Type type, DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    DeclaredTypeDescriptor typeDescriptor = type.getTypeDescriptor();

    // Make constructors defer to the JsFunctionAdaptor constructor passing the function
    // references to the implementing function and bridge.
    if (type.getConstructors().isEmpty()) {
      type.addMember(
          0,
          Method.builder()
              .setMethodDescriptor(AstUtils.createImplicitConstructorDescriptor(typeDescriptor))
              .setSourcePosition(type.getSourcePosition())
              .build());
    }
    type.getConstructors()
        .forEach(
            constructor ->
                insertWasmFuncrefSuperconstructorCall(constructor, jsFunctionTypeDescriptor));
  }

  private static void insertWasmFuncrefSuperconstructorCall(
      Method constructor, DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    if (AstUtils.hasThisCall(constructor)) {
      return;
    }
    var superCall =
        createWasmFuncrefSuperconstructorCall(
            constructor.getSourcePosition(),
            constructor.getDescriptor().getEnclosingTypeDescriptor(),
            jsFunctionTypeDescriptor);
    if (AstUtils.hasSuperCall(constructor)) {
      // Since JsFunction impls cannot have a superclass, if there is an existing super() call, it's
      // a call to Object. Replace it with the JsFunctionAdaptor super call.
      constructor.getBody().getStatements().set(0, superCall);
      return;
    }
    constructor.getBody().getStatements().addFirst(superCall);
  }

  private static Statement createWasmFuncrefSuperconstructorCall(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor implementorTypeDescriptor,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    return MethodCall.builderFrom(
            TypeDescriptors.get()
                .javaemulInternalJsFunctionAdaptor
                .getMethodDescriptor(
                    MethodDescriptor.CONSTRUCTOR_METHOD_NAME,
                    TypeDescriptors.get().javaemulInternalWasmFuncref,
                    TypeDescriptors.get().javaemulInternalWasmFuncref))
        .setArguments(
            AstUtils.createWasmFuncrefExpression(
                sourcePosition,
                getWasmFunctionPointerTarget(implementorTypeDescriptor, jsFunctionTypeDescriptor),
                jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor()),
            AstUtils.createWasmFuncrefExpression(
                sourcePosition,
                WasmExportBridgesUtils.getJsFunctionBridgeDescriptor(jsFunctionTypeDescriptor),
                jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor()))
        .build()
        .makeStatement(sourcePosition);
  }

  /**
   * Retrieves the method descriptor to be used as the target for the Wasm function pointer.
   *
   * <p>When an implementor implements a generic JsFunction interface (e.g. {@code
   * MyJsFunction<T>}), the generated calling logic only knows the unspecialized signature of the
   * method and performs a cast to that unspecialized function type. In this case, the generalizing
   * bridge method is used as the target.
   */
  private static MethodDescriptor getWasmFunctionPointerTarget(
      DeclaredTypeDescriptor implementorTypeDescriptor,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    MethodDescriptor lambdaMethodDescriptor =
        getJsFunctionMethodDescriptor(implementorTypeDescriptor, jsFunctionTypeDescriptor);
    for (MethodDescriptor methodDescriptor : implementorTypeDescriptor.getPolymorphicMethods()) {
      if (methodDescriptor.isGeneralizingBridge()
          && methodDescriptor.getBridgeTarget().equals(lambdaMethodDescriptor)) {
        return methodDescriptor;
      }
    }
    return lambdaMethodDescriptor;
  }

  private static MethodDescriptor getJsFunctionMethodDescriptor(
      DeclaredTypeDescriptor implementorTypeDescriptor,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    MethodDescriptor functionalInterfaceMethodDescriptor =
        jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor();
    return implementorTypeDescriptor.getDeclaredMethodDescriptors().stream()
        .filter(
            methodDescriptor -> methodDescriptor.isOverride(functionalInterfaceMethodDescriptor))
        .findFirst()
        .get();
  }
}
