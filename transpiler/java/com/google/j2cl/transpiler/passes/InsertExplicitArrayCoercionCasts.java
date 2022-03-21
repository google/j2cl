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

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Inserts explicit array coercion casts.
 *
 * <p>Array types are covariant in Java and invariant in Kotlin from the perspective of type
 * checking however, at runtime their assignability follows Java semantics, allowing assignment of
 * Java compatible array types if they are cast.
 */
public class InsertExplicitArrayCoercionCasts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                TypeDescriptor typeDescriptor = expression.getTypeDescriptor().toNonNullable();
                TypeDescriptor castTypeDescriptor = inferredTypeDescriptor.toNonNullable();
                return inferredTypeDescriptor.isArray()
                        && !castTypeDescriptor.equals(typeDescriptor)
                    ? CastExpression.newBuilder()
                        .setExpression(expression)
                        .setCastTypeDescriptor(castTypeDescriptor)
                        .build()
                    : expression;
              }
            }));

    // Kotlin type of vararg argument is "Array<out T>", so it needs cast to "Array<T>" to make it
    // writable.
    // TODO(b/223232961): Revisit this once we can express variance.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            if (!method.getDescriptor().isVarargs()) {
              return method;
            }

            Variable varargVariable = Iterables.getLast(method.getParameters());
            ArrayTypeDescriptor arrayTypeDescriptor =
                (ArrayTypeDescriptor) varargVariable.getTypeDescriptor();
            if (arrayTypeDescriptor.getComponentTypeDescriptor().isPrimitive()) {
              return method;
            }

            method.accept(
                new AbstractRewriter() {
                  @Override
                  public Node rewriteVariableReference(VariableReference variableReference) {
                    return variableReference.getTarget() == varargVariable
                        ? CastExpression.newBuilder()
                            .setExpression(variableReference)
                            .setCastTypeDescriptor(variableReference.getTypeDescriptor())
                            .build()
                        : variableReference;
                  }
                });

            return method;
          }
        });
  }
}
