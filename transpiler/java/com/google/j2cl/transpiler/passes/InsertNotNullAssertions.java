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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;
import java.util.function.Function;

/**
 * Inserts NOT_NULL_ASSERTION (!!) in places where Java performs implicit null-check, and when
 * conversion is needed from nullable to non-null type.
 */
public final class InsertNotNullAssertions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
            return ArrayAccess.Builder.from(arrayAccess)
                .setArrayExpression(
                    insertNotNullAssertionIfNeeded(arrayAccess.getArrayExpression()))
                .build();
          }

          @Override
          public Node rewriteArrayLength(ArrayLength arrayLength) {
            return ArrayLength.Builder.from(arrayLength)
                .setArrayExpression(
                    insertNotNullAssertionIfNeeded(arrayLength.getArrayExpression()))
                .build();
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            return FieldAccess.Builder.from(fieldAccess)
                .setQualifier(
                    applyIfNotNull(
                        fieldAccess.getQualifier(),
                        InsertNotNullAssertions::insertNotNullAssertionIfNeeded))
                .build();
          }

          @Override
          public Node rewriteForEachStatement(ForEachStatement forEachStatement) {
            return ForEachStatement.Builder.from(forEachStatement)
                .setIterableExpression(
                    insertNotNullAssertionIfNeeded(forEachStatement.getIterableExpression()))
                .build();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            return MethodCall.Builder.from(methodCall)
                .setQualifier(
                    applyIfNotNull(
                        methodCall.getQualifier(),
                        InsertNotNullAssertions::insertNotNullAssertionIfNeeded))
                .build();
          }

          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            return NewInstance.Builder.from(newInstance)
                .setQualifier(
                    applyIfNotNull(
                        newInstance.getQualifier(),
                        InsertNotNullAssertions::insertNotNullAssertionIfNeeded))
                .build();
          }

          @Override
          public Node rewriteSwitchStatement(SwitchStatement switchStatement) {
            return SwitchStatement.Builder.from(switchStatement)
                .setSwitchExpression(
                    insertNotNullAssertionIfNeeded(switchStatement.getSwitchExpression()))
                .build();
          }

          @Override
          public Node rewriteSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
            return SynchronizedStatement.Builder.from(synchronizedStatement)
                .setExpression(
                    insertNotNullAssertionIfNeeded(synchronizedStatement.getExpression()))
                .build();
          }

          @Override
          public Node rewriteThrowStatement(ThrowStatement throwStatement) {
            return ThrowStatement.Builder.from(throwStatement)
                .setExpression(insertNotNullAssertionIfNeeded(throwStatement.getExpression()))
                .build();
          }
        });

    // Insert non-null assertions when converting from nullable to non-null type.
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                return expression.canBeNull()
                        && (!inferredTypeDescriptor.canBeNull()
                            || !actualTypeDescriptor.canBeNull())
                    ? insertNotNullAssertion(expression)
                    : expression;
              }
            }));
  }

  private static boolean doesNotNeedNullCheck(Expression expression) {
    // Don't insert null-check for expressions which are known to be non-null, regardless of
    // nullability annotations.
    return doesNotNeedNullCheck(expression.getTypeDescriptor())
        || expression instanceof ThisOrSuperReference
        || expression instanceof NewInstance
        || expression instanceof ArrayLiteral
        || expression instanceof FunctionExpression
        || expression instanceof NewArray
        || expression instanceof Literal
        || (expression instanceof FieldAccess
            && ((FieldAccess) expression).getTarget().isEnumConstant());
  }

  private static boolean doesNotNeedNullCheck(TypeDescriptor typeDescriptor) {
    // Don't insert null-check for types which are known to be non-null, regardless of
    // nullability annotations.
    return typeDescriptor.isPrimitive();
  }

  private static Expression insertNotNullAssertionIfNeeded(Expression expression) {
    return !doesNotNeedNullCheck(expression) ? insertNotNullAssertion(expression) : expression;
  }

  private static Expression insertNotNullAssertion(Expression expression) {
    return PostfixExpression.newBuilder()
        .setOperand(expression)
        .setOperator(PostfixOperator.NOT_NULL_ASSERTION)
        .build();
  }

  private static <T> T applyIfNotNull(T t, Function<T, T> fn) {
    return t == null ? null : fn.apply(t);
  }
}
