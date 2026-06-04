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
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
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
 * <p>For {@code JsFunction} interfaces, it creates concrete adapter classes to facilitate passing
 * JsFunctions between Wasm and JavaScript. The generated adapter class holds a JavaScript function
 * reference (an {@code externref}), and allows it to be called in Wasm.
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
 * <pre>{@code
 * class F.JsFunctionAdaptor {
 *   WasmExtern jsFuncref;
 *
 *   JsFunctionAdaptor(WasmExtern jsFuncref) {
 *     this.jsFuncref = jsFuncref;
 *   }
 *
 *   JsFunctionAdaptor() {}
 *
 *   R method(String param) {
 *     return call(jsFuncref, param);
 *   }
 * }
 * }</pre>
 *
 * <p>For other functional interfaces, it creates abstract classes that implement the functional
 * interfaces to be shared superclasses of all lambda implementors.
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
              addDefaultConstructor(adaptorType);
              addJsFuncrefField(adaptorType);
              addJsFuncrefConstructor(adaptorType);
              addJsFunctionInvokeMethod(adaptorType);
              addJsFunctionForwardingMethod(adaptorType);
            }

            functionalInterfaceAdaptors.add(adaptorType);
          }
        });
    compilationUnit.addTypes(functionalInterfaceAdaptors);
  }

  /**
   * Adds a default constructor to the JsFunction adaptor class to be used by subclasses:
   * Wasm-originating JS functions and lambda implementors.
   */
  private static void addDefaultConstructor(Type adaptorType) {
    DeclaredTypeDescriptor adaptorTypeDescriptor = adaptorType.getTypeDescriptor();

    // Generates:
    // JsFunctionAdaptor() {}
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(
                LambdaAdaptorTypeDescriptors.getLambdaAdaptorDefaultConstructor(
                    adaptorTypeDescriptor))
            .setSourcePosition(adaptorType.getSourcePosition())
            .build());
  }

  /** Adds a field to the JsFunction adaptor class to store the JavaScript function reference. */
  private static void addJsFuncrefField(Type adaptorType) {
    adaptorType.addMember(
        Field.builderFrom(
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorJsFuncrefField(
                    adaptorType.getTypeDescriptor()))
            .setSourcePosition(adaptorType.getSourcePosition())
            .build());
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

    // Generates:
    // JsFunctionAdaptor(WasmExtern jsFuncref) {
    //   this.jsFuncref = jsFuncref;
    // }
    adaptorType.addMember(
        Method.builder()
            .setMethodDescriptor(
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorConstructor(
                    adaptorTypeDescriptor))
            .setParameters(wasmExternParameter)
            .addStatements(
                FieldAccess.builderFrom(
                        LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorJsFuncrefField(
                            adaptorTypeDescriptor))
                    .setDefaultInstanceQualifier()
                    .build()
                    .infixAssign(wasmExternParameter.createReference())
                    .makeStatement(sourcePosition))
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
                LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptorJsFuncrefField(
                    adaptorTypeDescriptor))
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
