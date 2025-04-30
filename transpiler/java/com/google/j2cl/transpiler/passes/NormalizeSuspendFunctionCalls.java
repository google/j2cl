/*
 * Copyright 2025 Google Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;

/** Rewrite suspend function calls to pass the continuation object as the first arguments. */
public class NormalizeSuspendFunctionCalls extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isSuspendFunction()) {
              return methodCall;
            }

            return MethodCall.Builder.from(methodCall)
                .setArguments(
                    new ImmutableList.Builder<Expression>()
                        .add(getContinuationParameterInScope().createReference())
                        .addAll(methodCall.getArguments())
                        .build())
                .build();
          }

          /**
           * Returns the continuation parameter of the most enclosing suspend function or lambda.
           */
          private Variable getContinuationParameterInScope() {
            MethodLike enclosinMethodLike =
                getParents()
                    .filter(MethodLike.class::isInstance)
                    .map(MethodLike.class::cast)
                    .findFirst()
                    .orElseThrow();
            // The enclosing method or lambda is guaranteed to be an suspend function.
            checkState(enclosinMethodLike.getDescriptor().isSuspendFunction());

            Variable continuationParameter = enclosinMethodLike.getParameters().getFirst();
            checkState(
                continuationParameter
                    .getTypeDescriptor()
                    .hasSameRawType(TypeDescriptors.get().kotlinCoroutinesContinuation));
            return continuationParameter;
          }
        });
  }
}
