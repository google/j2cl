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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates adapter classes for functional interfaces.
 *
 * <p>In general, the pass creates abstract classes that implement the functional interface to be
 * shared superclasses of all lambda implementors.
 *
 * <p>For {@code JsFunction} interfaces, the adapter class facilitates passing JsFunctions between
 * Wasm and JavaScript.
 *
 * <p>This adapter class is shared by both JS- and Wasm-originating JsFunction instances. For JS-
 * originating JsFunction instances, the generated adapter class holds a JavaScript function
 * reference (an {@code externref}), and allows it to be called in Wasm. For Wasm-originating
 * JsFunction instances, the adapter class holds a Wasm function reference (a {@code funcref}).
 *
 * <p>Given:
 *
 * <pre>{@code
 * @JsFunction
 * interface F {
 *   R method(String param);
 * }
 * }</pre>
 *
 * <p>Generates:
 *
 * <p>TODO(b/537884836): Update the example after cleaning up the type heirarchy. Since this
 * intermediate type is never instantiated, it makes sense to remove it from the tree.
 *
 * <pre>{@code
 * class F.JsFunctionAdaptor extends javaemul.internal.JsFunctionAdaptor {
 *   JsFunctionAdaptor(WasmExtern jsFuncref) {
 *     super(jsFuncref);
 *   }
 *
 *   JsFunctionAdaptor(WasmFuncref wasmFuncref) {
 *     super(wasmFuncref);
 *   }
 *
 *   R method(String param) {
 *     return invoke(jsFuncref, param);
 *   }
 *
 *   @JsMethod(...)
 *   native R invoke(WasmExtern jsFuncref, String param);
 * }
 * }</pre>
 */
public class AddFunctionalInterfaceAdaptors extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    List<Type> functionalInterfaceAdaptors = new ArrayList<>();
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            TypeDeclaration typeDeclaration = type.getDeclaration();
            if (!typeDeclaration.isFunctionalInterface()
                // Native interfaces cannot be implemented in Wasm.
                || typeDeclaration.isNative()) {
              return;
            }

            DeclaredTypeDescriptor adaptorTypeDescriptor =
                LambdaAdaptorTypeDescriptors.createFunctionalInterfaceAdaptorTypeDescriptor(
                    type.getTypeDescriptor());
            Type adaptorType =
                new Type(type.getSourcePosition(), adaptorTypeDescriptor.getTypeDeclaration());

            if (typeDeclaration.isJsFunctionInterface()) {
              addJsFuncrefConstructor(adaptorType);
              addWasmFuncrefConstructor(adaptorType);
              addJsFunctionAdaptMethod(adaptorType);
              addJsFunctionInvokeMethod(adaptorType);
              addJsFunctionForwardingMethod(adaptorType);
            }

            functionalInterfaceAdaptors.add(adaptorType);
          }
        });
    compilationUnit.addTypes(functionalInterfaceAdaptors);
  }

  /**
   * Adds a constructor to the JsFunction adaptor class that initializes a JavaScript function
   * reference.
   */
  private static void addJsFuncrefConstructor(Type adaptorType) {
    DeclaredTypeDescriptor adaptorTypeDescriptor = adaptorType.getTypeDescriptor();
    SourcePosition sourcePosition = adaptorType.getSourcePosition();
    Variable wasmExternParameter =
        Variable.builder()
            .setFinal(true)
            .setParameter(true)
            .setName("jsFuncref")
            .setTypeDescriptor(TypeDescriptors.get().javaemulInternalWasmExtern)
            .build();

    MethodDescriptor superConstructorDescriptor =
        TypeDescriptors.get()
            .javaemulInternalJsFunctionAdaptor
            .getMethodDescriptor(
                MethodDescriptor.CONSTRUCTOR_METHOD_NAME,
                TypeDescriptors.get().javaemulInternalWasmExtern);

    // Generates:
    // JsFunctionAdaptor(WasmExtern jsFuncref) {
    //   super(jsFuncref);
    // }
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorJsFuncrefConstructor(
                    adaptorTypeDescriptor))
            .setParameters(wasmExternParameter)
            .addStatements(
                MethodCall.builderFrom(superConstructorDescriptor)
                    .setArguments(wasmExternParameter.createReference())
                    .build()
                    .makeStatement(sourcePosition))
            .setSourcePosition(sourcePosition)
            .build());
  }

  /**
   * Adds a constructor to the JsFunction adaptor class that initializes a Wasm function reference.
   */
  private static void addWasmFuncrefConstructor(Type adaptorType) {
    DeclaredTypeDescriptor adaptorTypeDescriptor = adaptorType.getTypeDescriptor();
    SourcePosition sourcePosition = adaptorType.getSourcePosition();
    Variable wasmFuncrefParameter =
        Variable.builder()
            .setFinal(true)
            .setParameter(true)
            .setName("wasmFuncref")
            .setTypeDescriptor(TypeDescriptors.get().javaemulInternalWasmFuncref)
            .build();

    MethodDescriptor superConstructorDescriptor =
        TypeDescriptors.get()
            .javaemulInternalJsFunctionAdaptor
            .getMethodDescriptor(
                MethodDescriptor.CONSTRUCTOR_METHOD_NAME,
                TypeDescriptors.get().javaemulInternalWasmFuncref);

    // Generates:
    // JsFunctionAdaptor(WasmFuncref wasmFuncref) {
    //   super(wasmFuncref);
    // }
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorWasmFuncrefConstructor(
                    adaptorTypeDescriptor))
            .setParameters(wasmFuncrefParameter)
            .addStatements(
                MethodCall.builderFrom(superConstructorDescriptor)
                    .setArguments(wasmFuncrefParameter.createReference())
                    .build()
                    .makeStatement(sourcePosition))
            .setSourcePosition(sourcePosition)
            .build());
  }

  /**
   * Adds a static adapt method to the JsFunction adaptor class to convert an incoming JavaScript
   * function reference to JsFunction adaptor.
   */
  private static void addJsFunctionAdaptMethod(Type adaptorType) {
    DeclaredTypeDescriptor adaptorTypeDescriptor = adaptorType.getTypeDescriptor();
    SourcePosition sourcePosition = adaptorType.getSourcePosition();
    Variable wasmExternParameter =
        Variable.builder()
            .setFinal(true)
            .setParameter(true)
            .setName("jsFuncref")
            .setTypeDescriptor(TypeDescriptors.get().javaemulInternalWasmExtern)
            .build();

    // TODO(b/516900958): Implement once we decide the exact mechanism to link the adaptor and the
    // js function. Update the example.
    // Generates:
    // static JsFunctionAdaptor adapt(WasmExtern jsFuncref) {
    //   return null;
    // }
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptMethod(adaptorTypeDescriptor))
            .setParameters(wasmExternParameter)
            .addStatements(
                ReturnStatement.builder()
                    .setExpression(adaptorTypeDescriptor.getNullValue())
                    .setSourcePosition(sourcePosition)
                    .build())
            .setSourcePosition(sourcePosition)
            .build());
  }

  /**
   * Adds a native invoke method to the JsFunction adaptor class to call the underlying JavaScript
   * function.
   */
  private static void addJsFunctionInvokeMethod(Type adaptorType) {
    DeclaredTypeDescriptor adaptorTypeDescriptor = adaptorType.getTypeDescriptor();
    MethodDescriptor invokeMethodDescriptor =
        LambdaAdaptorTypeDescriptors.getWasmJsFunctionInvokeMethod(adaptorTypeDescriptor);

    // Generates:
    // @JsMethod(namespace = "j2wasm.JsInteropRuntime", name = "invokeJsFunction")
    // native R invoke(WasmExtern jsFuncref, A a, B b, ...);
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(invokeMethodDescriptor)
            .setParameters(
                AstUtils.createParameterVariables(
                    invokeMethodDescriptor.getParameterTypeDescriptors()))
            .setSourcePosition(adaptorType.getSourcePosition())
            .build());
  }

  /** Adds a method to the JsFunction adaptor class to forward to the JavaScript function. */
  private static void addJsFunctionForwardingMethod(Type adaptorType) {
    DeclaredTypeDescriptor adaptorTypeDescriptor = adaptorType.getTypeDescriptor();
    SourcePosition sourcePosition = adaptorType.getSourcePosition();
    MethodDescriptor forwardingMethodDescriptor =
        LambdaAdaptorTypeDescriptors.getAdaptorForwardingMethod(adaptorTypeDescriptor);
    List<Variable> forwardedVariables =
        AstUtils.createParameterVariables(forwardingMethodDescriptor.getParameterTypeDescriptors());

    List<Expression> invokeArguments = new ArrayList<>();
    invokeArguments.add(
        FieldAccess.builderFrom(
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorJsFuncrefField())
            .setQualifier(new ThisReference(adaptorTypeDescriptor))
            .build());
    forwardedVariables.forEach(param -> invokeArguments.add(param.createReference()));

    // R method(A a, B b, ...) {
    //   // Call the JavaScript function using an imported helper.
    //   return invoke(this.jsFuncref, a, b, ...);
    // }
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(forwardingMethodDescriptor)
            .setParameters(forwardedVariables)
            .addStatements(
                AstUtils.createReturnOrExpressionStatement(
                    sourcePosition,
                    MethodCall.builderFrom(
                            LambdaAdaptorTypeDescriptors.getWasmJsFunctionInvokeMethod(
                                adaptorTypeDescriptor))
                        .setArguments(invokeArguments)
                        .build(),
                    forwardingMethodDescriptor.getReturnTypeDescriptor()))
            .setSourcePosition(sourcePosition)
            .build());
  }
}
