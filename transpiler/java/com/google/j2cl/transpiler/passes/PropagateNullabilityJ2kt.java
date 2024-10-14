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
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import java.util.Map;

/** Propagates nullability in overrides in non-null-marked types. */
public class PropagateNullabilityJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            // Don't propagate nullability in null-marked types, since it's assumed to be correct
            // there. Propagate nullability in non-null-marked types only.
            if (getCurrentType().getDeclaration().isNullMarked()) {
              return method;
            }
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
        .filter(md -> md.getEnclosingTypeDescriptor().getTypeDeclaration().isNullMarked())
        .findFirst()
        .map(
            overriddenMethodDescriptor ->
                propagateNullability(
                    overriddenMethodDescriptor.getDeclarationDescriptor(), methodDescriptor))
        .orElse(methodDescriptor);
  }

  private static MethodDescriptor propagateNullability(MethodDescriptor from, MethodDescriptor to) {
    Map<TypeVariable, TypeDescriptor> parametrization =
        to.getEnclosingTypeDescriptor().getParameterization();
    return MethodDescriptor.Builder.from(to)
        .setReturnTypeDescriptor(
            propagateReturnTypeNullability(
                specialize(parametrization, from.getReturnTypeDescriptor()),
                to.getReturnTypeDescriptor()))
        .setParameterDescriptors(
            Streams.zip(
                    from.getParameterTypeDescriptors().stream(),
                    to.getParameterDescriptors().stream(),
                    (fromTd, toPd) ->
                        propagateParameterNullability(specialize(parametrization, fromTd), toPd))
                .collect(toImmutableList()))
        .build();
  }

  private static TypeDescriptor propagateReturnTypeNullability(
      TypeDescriptor from, TypeDescriptor to) {
    if (!from.canBeNull()) {
      // Only turn returns non-null from nullable but not the other way around.
      // That allows to keep the specialization in the overriding method and satisfies the
      // the covariant return rule.
      return to.toNonNullable();
    }

    return to;
  }

  private static ParameterDescriptor propagateParameterNullability(
      TypeDescriptor from, ParameterDescriptor toParameter) {
    TypeDescriptor to = toParameter.getTypeDescriptor();
    // Parameter nullability must match.
    if (from.isTypeVariable() && to.isTypeVariable()) {
      TypeVariable fromTypeVariable = (TypeVariable) from;
      TypeVariable toTypeVariable = (TypeVariable) to;

      return fromTypeVariable.getNullabilityAnnotation()
              == toTypeVariable.getNullabilityAnnotation()
          ? toParameter
          : toParameter.toBuilder()
              .setTypeDescriptor(
                  TypeVariable.Builder.from(toTypeVariable)
                      .setNullabilityAnnotation(fromTypeVariable.getNullabilityAnnotation())
                      .build())
              .build();
    } else {
      return toParameter.toBuilder().setTypeDescriptor(to.toNullable(from.isNullable())).build();
    }
  }

  private static void updateParametersFromDescriptor(Method method) {
    Streams.forEachPair(
        method.getDescriptor().getParameterDescriptors().stream(),
        method.getParameters().stream(),
        (descriptor, parameter) -> parameter.setTypeDescriptor(descriptor.getTypeDescriptor()));
  }

  private static TypeDescriptor specialize(
      Map<TypeVariable, TypeDescriptor> parametrization, TypeDescriptor parameter) {
    if (parameter instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) parameter;
      TypeDescriptor td = parametrization.get(typeVariable);
      if (td != null) {
        parameter = td;
        if (!typeVariable.canBeNull() && !(parameter instanceof TypeVariable)) {
          parameter = parameter.toNonNullable();
        }
      }
    }

    return parameter;
  }
}
