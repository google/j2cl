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
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import java.util.stream.Stream;

/**
 * Propagates nullability in inferred types from actual nullability in expressions.
 *
 * <p>At this moment it propagates nullability of inferred component types in array literals, by
 * inferring them from value expressions. Eventually, it'll also fix type arguments in invocations
 * by inferring them from arguments.
 *
 * <p>In the following statement:
 *
 * <pre>{@code
 * @Nullable String[] arr = {null};
 * }</pre>
 *
 * the inferred type of array literal is {@code String[]} instead of {@code @Nullable String[]}. It
 * causes insertion of {@code !!} in transpiled Kotlin code:
 *
 * <pre>{@code
 * val arr: Array<String?> = arrayOf<String>(null!!) as Array<String?>
 * }</pre>
 *
 * This pass fixes the inferred array component type to be {@code @Nullable String[]}, which fixes
 * the problem in transpiled Kotlin code:
 *
 * <pre>{@code
 * val arr: Array<String?> = arrayOf<String?>(null)
 * }</pre>
 */
public class PropagateNullability extends AbstractJ2ktNormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
            return propagateNullabilityFromValueExpressions(arrayLiteral);
          }

          @Override
          public Node rewriteNewArray(NewArray newArray) {
            Expression initializer = newArray.getInitializer();
            if (initializer == null) {
              return newArray;
            }
            // Update type of NewArray expression from rewritten initializer.
            return NewArray.Builder.from(newArray)
                .setTypeDescriptor((ArrayTypeDescriptor) initializer.getTypeDescriptor())
                .build();
          }
        });
  }

  private ArrayLiteral propagateNullabilityFromValueExpressions(ArrayLiteral arrayLiteral) {
    ArrayTypeDescriptor arrayTypeDescriptor = arrayLiteral.getTypeDescriptor();
    TypeDescriptor componentTypeDescriptor =
        propagateNullabilityFrom(
            arrayTypeDescriptor.getComponentTypeDescriptor(),
            arrayLiteral.getValueExpressions().stream().map(Expression::getTypeDescriptor));
    return arrayLiteral.toBuilder()
        .setTypeDescriptor(
            ArrayTypeDescriptor.Builder.from(arrayTypeDescriptor)
                .setComponentTypeDescriptor(componentTypeDescriptor)
                .build())
        .build();
  }

  private static TypeDescriptor propagateNullabilityFrom(
      TypeDescriptor typeDescriptor, TypeDescriptor from) {
    return !from.equals(typeDescriptor) && from.canBeNull()
        ? typeDescriptor.toNullable()
        : typeDescriptor;
  }

  private static TypeDescriptor propagateNullabilityFrom(
      TypeDescriptor typeDescriptor, Stream<TypeDescriptor> fromTypeDescriptors) {
    return fromTypeDescriptors.reduce(
        typeDescriptor, PropagateNullability::propagateNullabilityFrom, (a, b) -> a);
  }
}
