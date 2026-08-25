/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.WasmFuncrefCall;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforms invocations of JsFunction functional methods in Wasm into a static call to the
 * corresponding JsFunction forwarding method added to the interface itself.
 *
 * <p>When a call to a JsFunction method is encountered, e.g.:
 *
 * <pre>{@code
 * // Where SomeFunction is a JsFunction interface:
 * SomeFunction f = () -> { };
 * f.run(arg1, arg2);
 * }</pre>
 *
 * <p>It is transformed to:
 *
 * <pre>{@code
 * SomeFunction.run$jsFunction((JsFunctionAdaptor) f, arg1, arg2);
 * }</pre>
 *
 * <p>To facilitate this, and to reduce redundancy, static helper methods are added to the
 * interface:
 *
 * <pre>{@code
 * interface SomeFunction {
 *   // ...
 *
 *   static R run$jsFunction(javamul.internal.JsFunctionAdaptor adaptor, String param) {
 *     if (adaptor.wasmFuncref != null) {
 *       return adaptor.wasmFuncref(adaptor, param);
 *     } else {
 *       return $invokeJsFunction(adaptor.jsFuncref, param);
 *     }
 *   }
 *
 *   @JsMethod(...)
 *   native R $invokeJsFunction(WasmExtern jsFuncref, String param);
 * }
 * }</pre>
 */
public class ImplementJsFunctionInvocationsViaFunctionPointerCall extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    addHelperMethods(compilationUnit);
    rewriteCallsites(compilationUnit);
  }

  private static void addHelperMethods(CompilationUnit compilationUnit) {
    compilationUnit
        .streamTypes()
        .filter(type -> type.getTypeDescriptor().isJsFunctionInterface())
        .forEach(
            type -> {
              addJsInvokeMethod(type);
              addStaticForwardingMethod(type);
            });
  }

  private static void rewriteCallsites(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isJsFunction()) {
              return methodCall;
            }

            DeclaredTypeDescriptor jsFunctionInterface =
                methodCall.getTarget().getEnclosingTypeDescriptor().getFunctionalInterface();

            return MethodCall.builderFrom(getStaticForwardingMethodDescriptor(jsFunctionInterface))
                .setArguments(
                    ImmutableList.<Expression>builder()
                        .add(
                            CastExpression.builder()
                                .setExpression(methodCall.getQualifier())
                                // Note: This must be the base internal JsFunctionAdaptor class, not
                                // the specific JsFunction adapter subtype, because the given
                                // instance could be any JsFunction adapter type.
                                .setCastTypeDescriptor(
                                    TypeDescriptors.get().javaemulInternalJsFunctionAdaptor)
                                .build())
                        .addAll(methodCall.getArguments())
                        .build())
                .build();
          }
        });
  }

  /** Creates a native invoke method to call the underlying JavaScript function. */
  private static void addJsInvokeMethod(Type type) {
    DeclaredTypeDescriptor jsFunctionInterfaceType = type.getTypeDescriptor();
    checkArgument(jsFunctionInterfaceType.isJsFunctionInterface());
    MethodDescriptor invokeMethodDescriptor = getInvokeMethodDescriptor(jsFunctionInterfaceType);

    // Generates:
    // @JsMethod(namespace = "j2wasm.JsInteropRuntime", name = "invokeJsFunction")
    // native R invoke(WasmExtern jsFuncref, A a, B b, ...);
    type.addMember(
        Method.builder()
            .setMethodDescriptor(invokeMethodDescriptor)
            .setParameters(
                AstUtils.createParameterVariables(invokeMethodDescriptor.getParameterDescriptors()))
            .setSourcePosition(type.getSourcePosition())
            .build());
  }

  private static MethodDescriptor getInvokeMethodDescriptor(
      DeclaredTypeDescriptor jsFunctionInterface) {
    MethodDescriptor functionalMethod =
        jsFunctionInterface.getJsFunctionMethodDescriptor().getDeclarationDescriptor();

    return MethodDescriptor.builder()
        .setEnclosingTypeDescriptor(jsFunctionInterface)
        .setName("$invokeJsFunction")
        .setNative(true)
        .setStatic(true)
        .setParameterTypeDescriptors(
            ImmutableList.<TypeDescriptor>builder()
                .add(TypeDescriptors.get().javaemulInternalWasmExtern)
                .addAll(functionalMethod.getParameterTypeDescriptors())
                .build())
        .setReturnTypeDescriptor(functionalMethod.getReturnTypeDescriptor())
        .setOriginalJsInfo(
            JsInfo.builder()
                .setJsMemberType(JsMemberType.METHOD)
                .setJsNamespace("j2wasm.JsInteropRuntime")
                .setJsName("invokeJsFunction")
                .build())
        .build();
  }

  /**
   * Adds a static forwarding method that dispatches to either the Wasm function pointer or the
   * JavaScript function pointer depending on which one is present.
   */
  private static void addStaticForwardingMethod(Type type) {
    DeclaredTypeDescriptor jsFunctionInterfaceType = type.getTypeDescriptor();
    checkArgument(jsFunctionInterfaceType.isJsFunctionInterface());
    SourcePosition sourcePosition = type.getSourcePosition();
    MethodDescriptor staticForwardingMethodDescriptor =
        getStaticForwardingMethodDescriptor(jsFunctionInterfaceType);

    Variable jsFunctionInstance =
        Variable.builder()
            .setName("$instance")
            .setTypeDescriptor(
                TypeDescriptors.get().javaemulInternalJsFunctionAdaptor.toNonNullable())
            .setParameter(true)
            .setFinal(true)
            .build();

    List<Variable> forwardedVariables =
        AstUtils.createParameterVariables(
            staticForwardingMethodDescriptor
                .getParameterDescriptors()
                .subList(1, staticForwardingMethodDescriptor.getParameterDescriptors().size()));

    List<Variable> parameters = new ArrayList<>();
    parameters.add(jsFunctionInstance);
    parameters.addAll(forwardedVariables);

    TypeDescriptor returnTypeDescriptor =
        staticForwardingMethodDescriptor.getReturnTypeDescriptor();
    IfStatement ifStatement =
        IfStatement.builder()
            .setSourcePosition(sourcePosition)
            .setConditionExpression(
                getWasmFuncrefFieldAccess(jsFunctionInstance.createReference())
                    .infixNotEqualsNull())
            .setThenStatement(
                AstUtils.createReturnOrExpressionStatement(
                    sourcePosition,
                    createWasmFuncrefCall(
                        jsFunctionInterfaceType,
                        jsFunctionInstance.createReference(),
                        forwardedVariables),
                    returnTypeDescriptor))
            .setElseStatement(
                AstUtils.createReturnOrExpressionStatement(
                    sourcePosition,
                    createJsFuncrefCall(
                        jsFunctionInterfaceType,
                        jsFunctionInstance.createReference(),
                        forwardedVariables),
                    returnTypeDescriptor))
            .build();

    // Generates:
    // static R run(JsFunctionAdaptor adaptor, A a, B b, ...) {
    //   if (adaptor.wasmFuncref != null) {
    //     return adaptor.wasmFuncref(adaptor, a, b, ...);
    //   } else {
    //     return invoke(adaptor.jsFuncref, a, b, ...);
    //   }
    // }
    type.addMember(
        Method.builder()
            .setMethodDescriptor(staticForwardingMethodDescriptor)
            .setParameters(parameters)
            .addStatements(ifStatement)
            .setSourcePosition(sourcePosition)
            .build());
  }

  static MethodDescriptor getStaticForwardingMethodDescriptor(
      DeclaredTypeDescriptor jsFunctionInterfaceType) {
    MethodDescriptor functionalMethod =
        jsFunctionInterfaceType.getJsFunctionMethodDescriptor().getDeclarationDescriptor();

    return MethodDescriptor.builder()
        .setEnclosingTypeDescriptor(jsFunctionInterfaceType)
        .setName(functionalMethod.getName() + "$jsFunction")
        .setStatic(true)
        .setParameterTypeDescriptors(
            ImmutableList.<TypeDescriptor>builder()
                .add(TypeDescriptors.get().javaemulInternalJsFunctionAdaptor)
                .addAll(functionalMethod.getParameterTypeDescriptors())
                .build())
        .setReturnTypeDescriptor(functionalMethod.getReturnTypeDescriptor())
        .build();
  }

  private static WasmFuncrefCall createWasmFuncrefCall(
      DeclaredTypeDescriptor jsFunctionInterfaceType,
      Expression jsFunctionInstance,
      List<Variable> forwardedVariables) {
    ImmutableList<Expression> arguments =
        forwardedVariables.stream().map(Variable::createReference).collect(toImmutableList());

    return WasmFuncrefCall.builder()
        .setInstance(jsFunctionInstance.clone())
        .setFunctionalInterface(jsFunctionInterfaceType)
        .setFuncref(getWasmFuncrefFieldAccess(jsFunctionInstance))
        .setArguments(arguments)
        .build();
  }

  private static Expression getWasmFuncrefFieldAccess(Expression qualifier) {
    return FieldAccess.builder()
        .setQualifier(qualifier)
        .setTarget(
            TypeDescriptors.get()
                .javaemulInternalJsFunctionAdaptor
                .getFieldDescriptor("wasmFuncref"))
        .build();
  }

  private static MethodCall createJsFuncrefCall(
      DeclaredTypeDescriptor jsFunctionInterfaceType,
      Expression jsFunctionInstance,
      List<Variable> forwardedVariables) {
    List<Expression> jsFuncrefArguments = new ArrayList<>();
    jsFuncrefArguments.add(getJsFuncrefFieldAccess(jsFunctionInstance));
    forwardedVariables.forEach(param -> jsFuncrefArguments.add(param.createReference()));

    return MethodCall.builderFrom(getInvokeMethodDescriptor(jsFunctionInterfaceType))
        .setArguments(jsFuncrefArguments)
        .build();
  }

  private static Expression getJsFuncrefFieldAccess(Expression qualifier) {
    return FieldAccess.builder()
        .setQualifier(qualifier)
        .setTarget(
            TypeDescriptors.get().javaemulInternalJsFunctionAdaptor.getFieldDescriptor("jsFuncref"))
        .build();
  }
}
