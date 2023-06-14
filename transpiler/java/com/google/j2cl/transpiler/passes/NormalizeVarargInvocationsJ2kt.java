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
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;

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
            Expression lastArgument = Iterables.getLast(invocation.getArguments());

            // If the last argument is an array literal, or an array creation with array literal,
            // unwrap array literal, and pass the unwrapped arguments directly.
            if (lastArgument instanceof ArrayLiteral) {
              return Invocation.Builder.from(invocation)
                  .replaceVarargsArgument(((ArrayLiteral) lastArgument).getValueExpressions())
                  .build();
            }

            if (lastArgument instanceof NewArray) {
              Expression initializer = ((NewArray) lastArgument).getInitializer();
              if (initializer != null) {
                return Invocation.Builder.from(invocation)
                    .replaceVarargsArgument(((ArrayLiteral) initializer).getValueExpressions())
                    .build();
              }
            }

            return MethodCall.Builder.from(invocation)
                .replaceVarargsArgument(lastArgument.postfixNotNullAssertion().prefixSpread())
                .build();
          }
        });
  }
}
