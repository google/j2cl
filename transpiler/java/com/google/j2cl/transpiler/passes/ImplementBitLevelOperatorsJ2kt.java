/*
 * Copyright 2021 Google Inc.
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
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;

/** Rewrites certain Java operators to Kotlin method calls. */
public final class ImplementBitLevelOperatorsJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            switch (binaryExpression.getOperator()) {
              case BIT_AND:
                return rewriteToMethodCall(binaryExpression, "and");
              case BIT_OR:
                return rewriteToMethodCall(binaryExpression, "or");
              case BIT_XOR:
                return rewriteToMethodCall(binaryExpression, "xor");
              case LEFT_SHIFT:
                return rewriteToMethodCall(binaryExpression, "shl");
              case RIGHT_SHIFT_SIGNED:
                return rewriteToMethodCall(binaryExpression, "shr");
              case RIGHT_SHIFT_UNSIGNED:
                return rewriteToMethodCall(binaryExpression, "ushr");
              default:
                return binaryExpression;
            }
          }

          @Override
          public Node rewritePrefixExpression(PrefixExpression prefixExpression) {
            switch (prefixExpression.getOperator()) {
              case COMPLEMENT:
                return rewriteToMethodCall(prefixExpression, "inv");
              default:
                return prefixExpression;
            }
          }
        });
  }

  private static Expression rewriteToMethodCall(
      BinaryExpression binaryExpression, String methodName) {
    MethodDescriptor methodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(KOTLIN_BASIC_TYPE)
            .setReturnTypeDescriptor(binaryExpression.getTypeDescriptor())
            .setName(methodName)
            .setParameterTypeDescriptors(binaryExpression.getRightOperand().getTypeDescriptor())
            .build();

    return MethodCall.Builder.from(methodDescriptor)
        .setQualifier(binaryExpression.getLeftOperand())
        .setArguments(binaryExpression.getRightOperand())
        .build();
  }

  private static Expression rewriteToMethodCall(
      PrefixExpression prefixExpression, String methodName) {
    MethodDescriptor methodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(KOTLIN_BASIC_TYPE)
            .setReturnTypeDescriptor(prefixExpression.getTypeDescriptor())
            .setName(methodName)
            .build();

    return MethodCall.Builder.from(methodDescriptor)
        .setQualifier(prefixExpression.getOperand())
        .build();
  }

  private static final DeclaredTypeDescriptor KOTLIN_BASIC_TYPE =
      DeclaredTypeDescriptor.newBuilder()
          .setTypeDeclaration(
              TypeDeclaration.newBuilder()
                  .setKind(Kind.CLASS)
                  .setPackageName("j2kt")
                  .setClassComponents(ImmutableList.of("BasicType"))
                  .build())
          .build();
}
