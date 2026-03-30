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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Implements native array operations with calls to JavaScript.
 *
 * <p>Since Wasm does not natively support JavaScript arrays, operations on arrays typed as native
 * JsTypes must be delegated to JavaScript interop methods. The arrays themselves are represented as
 * opaque externrefs (WasmExtern) in Wasm.
 */
public class ImplementNativeJsTypeArrayOperationsWasm extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Rewrite setters before getters to avoid obscuring ArrayAccess in assignments (e.g. a[i] = b).
    rewriteSetters(compilationUnit);
    // Rewrite getters (e.g. a[i] and a.length).
    rewriteGetters(compilationUnit);
  }

  private void rewriteSetters(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (!(binaryExpression.getLeftOperand() instanceof ArrayAccess lhs)) {
              return binaryExpression;
            }
            if (!lhs.getArrayExpression().getTypeDescriptor().isNativeJsArray()) {
              return binaryExpression;
            }
            if (!binaryExpression.isSimpleAssignment()) {
              // Native JS arrays cannot be part of compound assignments.
              checkState(!binaryExpression.getOperator().isCompoundAssignment());
              return binaryExpression;
            }

            return MethodCall.Builder.from(
                    TypeDescriptors.get()
                        .javaemulInternalWasmExtern
                        .getMethodDescriptorByName("setArrayAt"))
                .setArguments(
                    lhs.getArrayExpression(),
                    lhs.getIndexExpression(),
                    binaryExpression.getRightOperand())
                .build();
          }
        });
  }

  private void rewriteGetters(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public Node rewriteArrayLength(ArrayLength arrayLength) {
            if (!arrayLength.getArrayExpression().getTypeDescriptor().isNativeJsArray()) {
              return arrayLength;
            }

            return MethodCall.Builder.from(
                    TypeDescriptors.get()
                        .javaemulInternalWasmExtern
                        .getMethodDescriptorByName("getArrayLength"))
                .setArguments(arrayLength.getArrayExpression())
                .build();
          }

          @Override
          public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
            if (!arrayAccess.getArrayExpression().getTypeDescriptor().isNativeJsArray()) {
              return arrayAccess;
            }

            return CastExpression.newBuilder()
                .setExpression(
                    MethodCall.Builder.from(
                            TypeDescriptors.get()
                                .javaemulInternalWasmExtern
                                .getMethodDescriptorByName("getArrayAt"))
                        .setArguments(
                            arrayAccess.getArrayExpression(), arrayAccess.getIndexExpression())
                        .build())
                .setCastTypeDescriptor(arrayAccess.getTypeDescriptor())
                .build();
          }
        });
  }
}
