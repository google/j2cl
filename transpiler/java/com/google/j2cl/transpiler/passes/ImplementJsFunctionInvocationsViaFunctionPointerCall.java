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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Transforms invocations of JsFunction functional methods in Wasm into a static call to the
 * corresponding JsFunction adaptor forwarding method.
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
 * SomeFunction.JsFunctionAdaptor.run((JsFunctionAdaptor) f, arg1, arg2);
 * }</pre>
 */
public class ImplementJsFunctionInvocationsViaFunctionPointerCall extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isJsFunction()) {
              return methodCall;
            }

            DeclaredTypeDescriptor specificAdaptorTypeDescriptor =
                LambdaAdaptorTypeDescriptors.createFunctionalInterfaceAdaptorTypeDescriptor(
                    methodCall
                        .getTarget()
                        .getEnclosingTypeDescriptor()
                        .getDeclarationDescriptor()
                        .getFunctionalInterface());

            return MethodCall.builderFrom(
                    LambdaAdaptorTypeDescriptors.getWasmJsFunctionStaticForwardingMethod(
                        specificAdaptorTypeDescriptor))
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
}
