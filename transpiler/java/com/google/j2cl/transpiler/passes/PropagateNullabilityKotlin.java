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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/** Propagates non-nullability of method return type in overrides in Kotlin. */
public class PropagateNullabilityKotlin extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            return propagateNullability(method);
          }
        });
  }

  private static Method propagateNullability(Method method) {
    method =
        Method.Builder.from(method)
            .setMethodDescriptor(propagateNullability(method.getDescriptor()))
            .build();
    updateParametersFromDescriptor(method);
    return method;
  }

  private static MethodDescriptor propagateNullability(MethodDescriptor methodDescriptor) {
    return methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
        .filter(MemberDescriptor::isNullabilityPropagationEnabled)
        .findFirst()
        .map(
            overriddenMethodDescriptor ->
                propagateNullability(overriddenMethodDescriptor, methodDescriptor))
        .orElse(methodDescriptor);
  }

  private static MethodDescriptor propagateNullability(MethodDescriptor from, MethodDescriptor to) {
    return MethodDescriptor.Builder.from(to)
        .setReturnTypeDescriptor(
            propagateReturnTypeNullability(
                from.getReturnTypeDescriptor(), to.getReturnTypeDescriptor()))
        .setParameterTypeDescriptors(
            Streams.zip(
                    from.getParameterTypeDescriptors().stream(),
                    to.getParameterTypeDescriptors().stream(),
                    PropagateNullabilityKotlin::propagateParameterNullability)
                .collect(toImmutableList()))
        .build();
  }

  private static TypeDescriptor propagateReturnTypeNullability(
      TypeDescriptor from, TypeDescriptor to) {
    if (!from.isNullable()) {
      // Only turn returns non-null from nullable but not the other way around.
      // That allows to keep the specialization in the overriding method and satisfies the
      // the covariant return rule.
      return to.toNonNullable();
    }

    return to;
  }

  private static TypeDescriptor propagateParameterNullability(
      TypeDescriptor from, TypeDescriptor to) {
    // Parameter nullability must match.
    return to.toNullable(from.isNullable());
  }

  private static void updateParametersFromDescriptor(Method method) {
    Streams.forEachPair(
        method.getDescriptor().getParameterDescriptors().stream(),
        method.getParameters().stream(),
        (descriptor, parameter) -> parameter.setTypeDescriptor(descriptor.getTypeDescriptor()));
  }
}
