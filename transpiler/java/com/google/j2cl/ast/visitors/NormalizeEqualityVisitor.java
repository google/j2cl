/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.ast.visitors;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;

/**
 * Replaces object == object expressions with Equality.$same(object, object) calls.
 */
public class NormalizeEqualityVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeEqualityVisitor().normalizeEquality(compilationUnit);
  }

  private void normalizeEquality(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
    // Don't rewrite non-equality expressions.
    if (binaryExpression.getOperator() != BinaryOperator.EQUALS
        && binaryExpression.getOperator() != BinaryOperator.NOT_EQUALS) {
      return binaryExpression;
    }

    // Don't rewrite primitive comparisons since '==' and '!=' are already good enough.
    if (binaryExpression.getLeftOperand().getTypeDescriptor().isPrimitive()
        || binaryExpression.getRightOperand().getTypeDescriptor().isPrimitive()) {
      return binaryExpression;
    }

    // Don't rewrite null literal comparisons since '==' and '!=' are already good enough.
    if (binaryExpression.getLeftOperand() instanceof NullLiteral
        || binaryExpression.getRightOperand() instanceof NullLiteral) {
      return binaryExpression;
    }

    // Rewrite object - object comparisons to avoid JS implicit conversions and still treat null and
    // undefined as equivalent.
    return MethodCall.createRegularMethodCall(
        null,
        binaryExpression.getOperator() == BinaryOperator.EQUALS
            ? AstUtils.createUtilSameMethodDescriptor()
            : AstUtils.createUtilNotSameMethodDescriptor(),
        Lists.newArrayList(binaryExpression.getLeftOperand(), binaryExpression.getRightOperand()));
  }
}
