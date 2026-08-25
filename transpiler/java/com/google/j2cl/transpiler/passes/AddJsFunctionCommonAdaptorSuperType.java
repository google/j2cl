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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;
import java.util.List;

/**
 * Normalizes concrete implementations of JsFunction interfaces by adapting them to extend the
 * common JsFunctionAdaptor type and synthesizing necessary bridging logic.
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

    // Add the common JsFunction adapter members.
    implementMembers(type, functionalInterfaceTypeDescriptor);
  }

  /**
   * Transforms the given JsFunction type to subclass {@code JsFunctionAdaptor} and expose necessary
   * external bridges.
   */
  private static void implementMembers(Type type, DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    DeclaredTypeDescriptor typeDescriptor = type.getTypeDescriptor();

    Method exportBridgeMethod =
        createExportBridgeMethod(
            type.getSourcePosition(), typeDescriptor, jsFunctionTypeDescriptor);
    type.addMember(exportBridgeMethod);

    // Make constructors defer to the JsFunctionAdaptor constructor passing the function
    // references
    // to the implementing function and bridge.
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
                insertWasmFuncrefSuperconstructorCall(
                    constructor, jsFunctionTypeDescriptor, exportBridgeMethod.getDescriptor()));
  }

  private static void insertWasmFuncrefSuperconstructorCall(
      Method constructor,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor,
      MethodDescriptor exportBridgeDescriptor) {
    if (AstUtils.hasThisCall(constructor)) {
      return;
    }
    var superCall =
        createWasmFuncrefSuperconstructorCall(
            constructor.getSourcePosition(),
            constructor.getDescriptor().getEnclosingTypeDescriptor(),
            jsFunctionTypeDescriptor,
            exportBridgeDescriptor);
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
      DeclaredTypeDescriptor jsFunctionTypeDescriptor,
      MethodDescriptor exportBridgeDescriptor) {
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
                exportBridgeDescriptor,
                jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor()))
        .build()
        .makeStatement(sourcePosition);
  }

  /**
   * Creates a static export bridge for JsFunction implementations which defers to the lambda
   * method.
   */
  private static Method createExportBridgeMethod(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor implementorTypeDescriptor,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    MethodDescriptor lambdaMethodDescriptor =
        getJsFunctionMethodDescriptor(implementorTypeDescriptor, jsFunctionTypeDescriptor);

    ImmutableList<TypeDescriptor> parameterTypes =
        ImmutableList.<TypeDescriptor>builder()
            // First parameter is the JsFunctionAdaptor instance (an externref for exporting).
            .add(
                WasmExportBridgesUtils.getExternalType(
                    TypeDescriptors.get().javaemulInternalJsFunctionAdaptor, /* isExport= */ false))
            .addAll(
                lambdaMethodDescriptor.getParameterTypeDescriptors().stream()
                    .map(t -> WasmExportBridgesUtils.getExternalType(t, /* isExport= */ false))
                    .iterator())
            .build();

    TypeDescriptor returnType =
        WasmExportBridgesUtils.getExternalType(
            lambdaMethodDescriptor.getReturnTypeDescriptor(), /* isExport= */ false);

    MethodDescriptor exportBridgeDescriptor =
        MethodDescriptor.builder()
            .setEnclosingTypeDescriptor(implementorTypeDescriptor)
            .setName(lambdaMethodDescriptor.getName() + "$export")
            .setStatic(true)
            .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_FUNCTION_EXPORT)
            .setParameterTypeDescriptors(parameterTypes)
            .setReturnTypeDescriptor(returnType)
            .build();

    List<Variable> parameters =
        AstUtils.createParameterVariables(exportBridgeDescriptor.getParameterDescriptors());

    // static R m$export(WasmExtern adaptor, A a, B b, ...) {
    //   return toJs(fromJs(adaptor).m(fromJs(a), fromJs(b), ...));
    // }
    Statement forwardingStatement =
        AstUtils.createForwardingStatement(
            sourcePosition,
            /* qualifier= */ WasmExportBridgesUtils.convertToInternal(
                parameters.get(0).createReference(),
                implementorTypeDescriptor,
                /* isExport= */ false),
            lambdaMethodDescriptor,
            /* isStaticDispatch= */ false,
            /* arguments= */ Streams.zip(
                    parameters.stream().skip(1),
                    lambdaMethodDescriptor.getParameterTypeDescriptors().stream(),
                    (parameter, typeDescriptor) ->
                        WasmExportBridgesUtils.convertToInternal(
                            parameter.createReference(), typeDescriptor, /* isExport= */ false))
                .collect(toImmutableList()),
            returnType);

    if (forwardingStatement instanceof ReturnStatement returnStatement) {
      forwardingStatement =
          returnStatement.toBuilder()
              .setExpression(
                  WasmExportBridgesUtils.convertToExternal(
                      returnStatement.getExpression(),
                      lambdaMethodDescriptor.getReturnTypeDescriptor(),
                      /* isExport= */ false))
              .build();
    }

    return Method.builder()
        .setMethodDescriptor(exportBridgeDescriptor)
        .setParameters(parameters)
        .addStatements(forwardingStatement)
        .setSourcePosition(sourcePosition)
        .build();
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
