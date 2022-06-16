/*
 * Copyright 2022 Google Inc.
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
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;

/** Propagates non-nullability of method return type in overrides in Kotlin. */
public class PropagateNullabilityKotlin extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            return Method.Builder.from(method)
                .setMethodDescriptor(propagateReturnTypeNullability(method.getDescriptor()))
                .build();
          }

          private MethodDescriptor propagateReturnTypeNullability(
              MethodDescriptor methodDescriptor) {
            if (methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
                .anyMatch(
                    m ->
                        m.isNullabilityPropagationEnabled()
                            && !m.getReturnTypeDescriptor().isPrimitive()
                            && !m.getReturnTypeDescriptor().isNullable())) {
              return MethodDescriptor.Builder.from(methodDescriptor)
                  .setReturnTypeDescriptor(
                      methodDescriptor.getReturnTypeDescriptor().toNonNullable())
                  .build();
            }

            return methodDescriptor;
          }
        });
  }
}
