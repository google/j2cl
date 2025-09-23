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
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fixes the nullability mismatches in anonymous class constructors. */
public class FixAnonymousClassInstantiations {
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            Type anonymousInnerClass = newInstance.getAnonymousInnerClass();
            if (anonymousInnerClass == null || anonymousInnerClass.getConstructors().isEmpty()) {
              return newInstance;
            }

            var constructor = Iterables.getOnlyElement(anonymousInnerClass.getConstructors());
            var superConstructorCall = checkNotNull(AstUtils.getConstructorInvocation(constructor));

            var constructorParameterMapping =
                getConstructorParameterMapping(superConstructorCall, constructor);

            updateConstructorMethodDescriptor(
                newInstance,
                constructor.getDescriptor(),
                superConstructorCall.getTarget(),
                constructorParameterMapping);

            return newInstance;
          }
        });
  }

  /**
   * Computes the mapping between the parameters in the synthetic constructor of the anonymous to
   * the parameters of the constructor of its superclass.
   *
   * <p>Although the parameters appear in the same order, there might me extra parameters preceding
   * or succeeding that correspond to captures.
   */
  private static Map<Integer, Integer> getConstructorParameterMapping(
      MethodCall superConstructorCall, Method constructor) {

    // Maps a superconstructor parameter indices to constructor parameter indices.
    Map<Integer, Integer> constructorParameterMapping = new HashMap<>();

    var superConstructorArguments = superConstructorCall.getArguments();
    for (int superConstructorParameterIndex = 0;
        superConstructorParameterIndex < superConstructorArguments.size();
        superConstructorParameterIndex++) {

      if (!(superConstructorArguments.get(superConstructorParameterIndex)
          instanceof VariableReference variableReference)) {
        // Not a variable reference, hence it is not reference to a parameter of the constructor.
        continue;
      }

      int constructorParameterIndex =
          constructor.getParameters().indexOf(variableReference.getTarget());

      if (constructorParameterIndex != -1) {
        // Found the actual parameter, record the mapping.
        constructorParameterMapping.put(superConstructorParameterIndex, constructorParameterIndex);
      }
    }
    return constructorParameterMapping;
  }

  /**
   * Propagate the nullability from the superconstructor parameters to the constructor parameters in
   * the anonymous classes.
   */
  private static void updateConstructorMethodDescriptor(
      NewInstance newInstance,
      MethodDescriptor constructor,
      MethodDescriptor superConstructor,
      Map<Integer, Integer> parameterMapping) {

    var superConstructorParameterTypes =
        superConstructor.getDeclarationDescriptor().getParameterTypeDescriptors();

    newInstance.accept(
        new AbstractRewriter() {
          @Override
          public MethodDescriptor rewriteMethodDescriptor(MethodDescriptor methodDescriptor) {
            if (methodDescriptor.getDeclarationDescriptor() != constructor) {
              return methodDescriptor;
            }

            var parameterDescriptors = updateParameterDescriptors(methodDescriptor);

            if (parameterDescriptors.equals(methodDescriptor.getParameterDescriptors())) {
              return methodDescriptor;
            }
            return MethodDescriptor.Builder.from(methodDescriptor)
                .setParameterDescriptors(parameterDescriptors)
                .build();
          }

          private List<ParameterDescriptor> updateParameterDescriptors(
              MethodDescriptor methodDescriptor) {
            var parameterDescriptors = new ArrayList<>(methodDescriptor.getParameterDescriptors());

            parameterMapping.forEach(
                (superParameterIndex, constructorParameterIndex) -> {
                  var declarationTypeDescriptor =
                      superConstructorParameterTypes.get(superParameterIndex);
                  var parameterDescriptor = parameterDescriptors.get(constructorParameterIndex);

                  var updatedParameterType =
                      JavaEnvironment.propagateNullability(
                              declarationTypeDescriptor, parameterDescriptor.getTypeDescriptor())
                          .toNullable(
                              declarationTypeDescriptor.isNullable()
                                  || parameterDescriptor.getTypeDescriptor().isNullable());

                  if (!updatedParameterType.equals(parameterDescriptor.getTypeDescriptor())) {
                    parameterDescriptors.set(
                        constructorParameterIndex,
                        parameterDescriptor.toBuilder()
                            .setTypeDescriptor(updatedParameterType)
                            .build());
                  }
                });
            return parameterDescriptors;
          }
        });
  }
}
