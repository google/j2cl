/*
 * Copyright 2023 Google Inc.
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
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import java.util.Collection;
import java.util.stream.Stream;

/** Normalize varargs invocations for Kotlin. */
public class NormalizeVarargInvocationsJ2kt extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInvocation(Invocation invocation) {
            MethodDescriptor target = invocation.getTarget();
            if (!target.isVarargs()) {
              return invocation;
            }
            Expression arrayExpression = Iterables.getLast(invocation.getArguments());

            // If the last argument is an array literal, unwrap it and pass arguments directly.
            if (arrayExpression instanceof ArrayLiteral) {
              ArrayLiteral arrayLiteral = (ArrayLiteral) arrayExpression;
              if (canUnwrapVarargArgument(invocation, arrayLiteral)) {
                return Invocation.Builder.from(invocation)
                    .replaceVarargsArgument(arrayLiteral.getValueExpressions())
                    .build();
              }
            }

            // Otherwise, apply spread operator to the array.
            return MethodCall.Builder.from(invocation)
                .replaceVarargsArgument(arrayExpression.postfixNotNullAssertion().prefixSpread())
                .build();
          }
        });
  }

  /** Returns whether vararg array literal argument can be unwrapped in Kotlin. */
  private static boolean canUnwrapVarargArgument(Invocation invocation, ArrayLiteral argument) {
    if (!argument.getValueExpressions().isEmpty()) {
      return true;
    }

    MethodDescriptor target = invocation.getTarget();
    DeclaredTypeDescriptor enclosingTypeDescriptor = target.getEnclosingTypeDescriptor();

    // Don't unwrap varargs in enum constructor calls, as they are... a bit convoluted.
    DeclaredTypeDescriptor superTypeDescriptor = enclosingTypeDescriptor.getSuperTypeDescriptor();
    if (superTypeDescriptor != null && superTypeDescriptor.isEnum()) {
      return false;
    }

    // Collect method descriptors to look for overloads.
    Collection<MethodDescriptor> methodDescriptors =
        target.isInstanceMember()
            ? enclosingTypeDescriptor.getPolymorphicMethods()
            // Overload resolution for static methods only considers method in the target
            // type, not supertypes.
            : enclosingTypeDescriptor.getDeclaredMethodDescriptors();

    // Don't unwrap if there are many overloads that matches the invocation.
    Stream<MethodDescriptor> matchingMethodDescriptors =
        methodDescriptors.stream()
            .filter(methodDescriptor -> isVarargAndMatches(methodDescriptor, invocation));

    return matchingMethodDescriptors.limit(2).count() <= 1;
  }

  /**
   * Returns true if the given method descriptor is vararg and matches the given invocation with
   * empty varargs.
   */
  private static boolean isVarargAndMatches(
      MethodDescriptor methodDescriptor, Invocation invocation) {
    return methodDescriptor.isVarargs()
        && methodDescriptor.getName().equals(invocation.getTarget().getName())
        && Streams.zip(
                methodDescriptor.getParameterDescriptors().stream(),
                invocation.getArguments().stream(),
                NormalizeVarargInvocationsJ2kt::isVarargOrMatches)
            .allMatch(it -> it);
  }

  /** Returns true if parameter is vararg, or non-vararg and matches the given argument. */
  private static boolean isVarargOrMatches(ParameterDescriptor parameter, Expression argument) {
    return parameter.isVarargs()
        || argument.getTypeDescriptor().isAssignableTo(parameter.getTypeDescriptor());
  }
}
