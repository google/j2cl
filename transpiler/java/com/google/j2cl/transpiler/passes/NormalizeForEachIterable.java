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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;

/** Normalizes iterable expressions in for each statements. */
public class NormalizeForEachIterable extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteForEachStatement(ForEachStatement forEachStatement) {
            return normalizeIterable(forEachStatement);
          }
        });
  }

  /** Normalizes the iterable expression so that it is never an union type. */
  private ForEachStatement normalizeIterable(ForEachStatement forEachStatement) {
    Expression iterableExpression = forEachStatement.getIterableExpression();
    if (!iterableExpression.getTypeDescriptor().isUnion()) {
      return forEachStatement;
    }

    Variable loopVariable = forEachStatement.getLoopVariable();
    UnionTypeDescriptor iterableType = (UnionTypeDescriptor) iterableExpression.getTypeDescriptor();
    // Union type descriptors do not support using `getMethodDescriptor` directly to determine the
    // type of the element. So use a helper that chooses a safe element type.
    TypeDescriptor targetElementType =
        computeTargetElementType(iterableType, loopVariable.getTypeDescriptor());
    return ForEachStatement.Builder.from(forEachStatement)
        .setIterableExpression(castToIterable(iterableExpression, targetElementType))
        .build();
  }

  /** Find the common iterable element type to cast to from the union type. */
  private static TypeDescriptor computeTargetElementType(
      UnionTypeDescriptor iterableType, TypeDescriptor loopVariableType) {

    ImmutableSet<TypeDescriptor> elementTypes =
        iterableType.getUnionTypeDescriptors().stream()
            .map(DeclaredTypeDescriptor.class::cast)
            .map(AstUtils::getIterableElement)
            .collect(toImmutableSet());

    if (elementTypes.size() == 1 || loopVariableType.isPrimitive()) {
      // All members of the union agree on the type of the element, or disagree in nullability.
      checkState(elementTypes.stream().map(TypeDescriptor::toNullable).distinct().count() == 1);
      return elementTypes.stream().findFirst().get();
    }

    // When the types don't agree, use the type of the loop variable. While this is not precise it
    // is safe to assume that type since there won't be any conversions nor coercions.
    return loopVariableType;
  }

  /** Generate cast to convert to an iterable of the right type. */
  private static Expression castToIterable(
      Expression expression, TypeDescriptor iterableElementType) {

    DeclaredTypeDescriptor iterableType = TypeDescriptors.get().javaLangIterable;
    return CastExpression.newBuilder()
        .setCastTypeDescriptor(
            iterableType.specializeTypeVariables(
                ImmutableMap.of(
                    Iterables.getOnlyElement(
                        iterableType.getTypeDeclaration().getTypeParameterDescriptors()),
                    iterableElementType)))
        .setExpression(expression)
        .build();
  }
}
