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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.OperatorSideEffectUtils;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Splits all compound assignments that are on primitive long variables.
 *
 * <p>The rewrite is needed because Longs are emulated and the emulation class instances are
 * immutable. If you left compound assignments in then you'd have to translate things like a++ into
 * a.increment(), but that would be invalid since a.increment() couldn't modify the value in itself.
 */
public class SplitCompoundLongAssignments extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator().isCompoundAssignment()
                && TypeDescriptors.isPrimitiveLong(
                    binaryExpression.getLeftOperand().getTypeDescriptor())
                && !(binaryExpression.getLeftOperand() instanceof ArrayAccess)) {
              return OperatorSideEffectUtils.splitBinaryExpression(binaryExpression);
            }
            return binaryExpression;
          }

          @Override
          public Expression rewritePrefixExpression(PrefixExpression prefixExpression) {
            if (prefixExpression.getOperator().hasSideEffect()
                && TypeDescriptors.isPrimitiveLong(
                    prefixExpression.getOperand().getTypeDescriptor())
                && !(prefixExpression.getOperand() instanceof ArrayAccess)) {
              return OperatorSideEffectUtils.splitPrefixExpression(prefixExpression);
            }
            return prefixExpression;
          }

          @Override
          public Expression rewritePostfixExpression(PostfixExpression postfixExpression) {
            if (TypeDescriptors.isPrimitiveLong(postfixExpression.getOperand().getTypeDescriptor())
                && !(postfixExpression.getOperand() instanceof ArrayAccess)) {
              return OperatorSideEffectUtils.splitPostfixExpression(postfixExpression);
            }
            return postfixExpression;
          }
        });
  }
}
