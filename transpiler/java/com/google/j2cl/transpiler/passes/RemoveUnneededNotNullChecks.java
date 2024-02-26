/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PostfixOperator;

/** Removes not-null postfix expressions where the runtime behavior isn't impacted. */
public class RemoveUnneededNotNullChecks extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
            var qualifier = arrayAccess.getArrayExpression();
            if (!isPostfixNotNullExpression(qualifier)) {
              return arrayAccess;
            }
            return ArrayAccess.Builder.from(arrayAccess)
                .setArrayExpression(((PostfixExpression) qualifier).getOperand())
                .build();
          }

          @Override
          public Node rewriteArrayLength(ArrayLength arrayLength) {
            var qualifier = arrayLength.getArrayExpression();
            if (!isPostfixNotNullExpression(qualifier)) {
              return arrayLength;
            }
            return ArrayLength.Builder.from(arrayLength)
                .setArrayExpression(((PostfixExpression) qualifier).getOperand())
                .build();
          }

          @Override
          public Node rewriteForEachStatement(ForEachStatement forEachStatement) {
            var iterableExpression = forEachStatement.getIterableExpression();
            if (!isPostfixNotNullExpression(iterableExpression)) {
              return forEachStatement;
            }
            return ForEachStatement.Builder.from(forEachStatement)
                .setIterableExpression(((PostfixExpression) iterableExpression).getOperand())
                .build();
          }

          @Override
          public Node rewriteMemberReference(MemberReference memberReference) {
            var qualifier = memberReference.getQualifier();
            if (!isPostfixNotNullExpression(qualifier)) {
              return memberReference;
            }
            return MemberReference.Builder.from(memberReference)
                .setQualifier(((PostfixExpression) qualifier).getOperand())
                .build();
          }

          @Override
          public Node rewritePostfixExpression(PostfixExpression postfixExpression) {
            var operand = postfixExpression.getOperand();
            if (isPostfixNotNullExpression(postfixExpression)
                && isPostfixNotNullExpression(operand)) {
              return operand;
            }
            return postfixExpression;
          }
        });
  }

  private static boolean isPostfixNotNullExpression(Expression expression) {
    return expression instanceof PostfixExpression
        && ((PostfixExpression) expression).getOperator() == PostfixOperator.NOT_NULL_ASSERTION;
  }
}
