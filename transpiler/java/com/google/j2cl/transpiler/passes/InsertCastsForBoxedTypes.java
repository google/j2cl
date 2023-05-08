/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isBoxedType;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A pass which inserts casts for boxed type when necessary, to dispatch to the corrent overload.
 *
 * <p>In Kotlin, primitive types are indistinguishable from non-null boxed types, which can lead to
 * incorrect method being called in case of overloads.
 *
 * <p>In example:
 *
 * <pre>{@code
 * class Overloads {
 *   static void apply(int i);
 *   static void apply(Object i);
 * }
 *
 * void test(int i1, Integer i2) {
 *   Overloads.apply(i1);
 *   Overloads.apply(i2);
 * }
 * }</pre>
 *
 * <p>Without an explicit cast, the code would be transpiled in Kotlin as:
 *
 * <pre>{@code
 * fun test(i1: Int, i2: Int) {
 *   Overloads.apply(i1);
 *   Overloads.apply(i2);
 * }
 * }</pre>
 *
 * Both method calls would dispatch to the {@code apply(int)} method. This pass will insert
 * necessary cast, so the code would be transpiled as:
 *
 * <pre>{@code
 * fun test(i1: Int, i2: Int) {
 *   Overloads.apply(i1);
 *   Overloads.apply(i2 as Object);
 * }
 * }</pre>
 */
public class InsertCastsForBoxedTypes extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInvocation(Invocation invocation) {
            ImmutableList<TypeDescriptor> declaredParameterTypeDescriptors =
                invocation.getTarget().getDeclarationDescriptor().getParameterTypeDescriptors();
            ImmutableList<TypeDescriptor> inferredParameterTypeDescriptors =
                invocation.getTarget().getParameterTypeDescriptors();
            List<Expression> arguments = invocation.getArguments();
            ImmutableList<Expression> rewrittenArguments =
                IntStream.range(0, invocation.getArguments().size())
                    .mapToObj(
                        i ->
                            rewriteInvocationArgument(
                                declaredParameterTypeDescriptors.get(i),
                                inferredParameterTypeDescriptors.get(i),
                                arguments.get(i)))
                    .collect(toImmutableList());
            return Invocation.Builder.from(invocation).setArguments(rewrittenArguments).build();
          }
        });
  }

  private static Expression rewriteInvocationArgument(
      TypeDescriptor declaredParameterTypeDescriptor,
      TypeDescriptor inferredParameterTypeDescriptor,
      Expression argument) {
    // TODO(b/279936148): Filter-out parameter types inferred from generic types, since
    // their nullability may be incorrect (they do not respect nullability of their bounds).
    TypeDescriptor argumentTypeDescriptor = argument.getTypeDescriptor();
    return declaredParameterTypeDescriptor instanceof DeclaredTypeDescriptor
            && isBoxed(argumentTypeDescriptor)
            && !argumentTypeDescriptor.equals(inferredParameterTypeDescriptor)
        ? CastExpression.newBuilder()
            .setExpression(argument)
            .setCastTypeDescriptor(inferredParameterTypeDescriptor)
            .build()
        : argument;
  }

  private static boolean isBoxed(TypeDescriptor typeDescriptor) {
    return typeDescriptor instanceof DeclaredTypeDescriptor && isBoxedType(typeDescriptor);
  }
}
