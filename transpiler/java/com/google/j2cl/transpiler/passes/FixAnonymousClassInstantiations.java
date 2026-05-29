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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.NullabilityPropagationUtils.propagateNullabilityTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Fixes the nullability mismatches in anonymous class constructors. */
public class FixAnonymousClassInstantiations extends NormalizationPass {
  @Override
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

            propagateFromMethods(newInstance);
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

    var superConstructorParameterTypes = superConstructor.getParameterTypeDescriptors();

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
            return methodDescriptor.toBuilder()
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
                  var parameterTypeDescriptor = parameterDescriptor.getTypeDescriptor();

                  if (declarationTypeDescriptor == parameterTypeDescriptor) {
                    // Same type, no need to propagate.
                    return;
                  }

                  var updatedParameterType =
                      propagateNullabilityTo(parameterTypeDescriptor, declarationTypeDescriptor)
                          .toNullable(
                              declarationTypeDescriptor.isNullable()
                                  || parameterTypeDescriptor.isNullable());

                  if (updatedParameterType != parameterTypeDescriptor) {
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

  /** Propagates nullability from the overridden methods of the anonymous class to its supertype. */
  private static void propagateFromMethods(NewInstance newInstance) {
    Type anonymousInnerClass = newInstance.getAnonymousInnerClass();
    if (!newInstance.getTypeArguments().isEmpty() || anonymousInnerClass == null) {
      return;
    }

    var superInterfaces = anonymousInnerClass.getSuperInterfaceTypeDescriptors();
    if (superInterfaces.isEmpty()) {
      // Propagate to superclass
      DeclaredTypeDescriptor superClass = anonymousInnerClass.getSuperTypeDescriptor();
      DeclaredTypeDescriptor newSuperClass =
          propagateFromMethods(anonymousInnerClass.getDeclaration(), superClass);
      if (!newSuperClass.equals(superClass)) {
        anonymousInnerClass.setSuperTypeDescriptor(newSuperClass);
      }
    } else {
      // Propagate to interfaces
      ImmutableList<DeclaredTypeDescriptor> newSuperInterfaces =
          superInterfaces.stream()
              .map(i -> propagateFromMethods(anonymousInnerClass.getDeclaration(), i))
              .collect(toImmutableList());
      if (!newSuperInterfaces.equals(superInterfaces)) {
        anonymousInnerClass.setSuperInterfaceTypeDescriptors(newSuperInterfaces);
      }
    }
  }

  /**
   * Propagates nullability from the methods of the anonymous class to the type arguments of the
   * given supertype descriptor.
   */
  private static DeclaredTypeDescriptor propagateFromMethods(
      TypeDeclaration anonymousClass, DeclaredTypeDescriptor superTypeDescriptor) {
    if (superTypeDescriptor.isRaw() || superTypeDescriptor.getTypeArgumentDescriptors().isEmpty()) {
      return superTypeDescriptor;
    }

    var typeParameterDescriptors =
        superTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors();
    var typeArgumentDescriptors = superTypeDescriptor.getTypeArgumentDescriptors();

    return superTypeDescriptor.withTypeArguments(
        Streams.zip(
                typeParameterDescriptors.stream(),
                typeArgumentDescriptors.stream(),
                (typeParameter, typeArgument) ->
                    propagateTypeArgumentNullabilityFromMethods(
                        typeParameter, typeArgument, anonymousClass))
            .collect(toImmutableList()));
  }

  /**
   * Propagates nullability to a specific type argument from the parameterizations found in the
   * methods of the anonymous class.
   */
  private static TypeDescriptor propagateTypeArgumentNullabilityFromMethods(
      TypeVariable typeParameterDescriptor,
      TypeDescriptor typeArgumentDescriptor,
      TypeDeclaration typeDeclaration) {
    return getParameterizationsFromMethods(typeDeclaration, typeParameterDescriptor)
        .reduce(
            typeParameterDescriptor.canBeNull()
                ? typeArgumentDescriptor
                : typeArgumentDescriptor.toNonNullable(),
            (typeArgument, typeDescriptor) ->
                propagateNullabilityTo(typeArgument, typeDescriptor)
                    .toNullable(typeArgument.isNullable() || typeDescriptor.isNullable()));
  }

  /**
   * Returns a stream of all parameterizations of the given type parameter found in the methods
   * overridden by the anonymous class.
   */
  private static Stream<TypeDescriptor> getParameterizationsFromMethods(
      TypeDeclaration typeDeclaration, TypeVariable typeParameter) {
    return typeDeclaration.getDeclaredMethodDescriptors().stream()
        .flatMap(
            m ->
                m.getJavaOverriddenMethodDescriptors().stream()
                    .flatMap(
                        overridden ->
                            getParameterizationsInMethod(
                                typeParameter, overridden.getDeclarationDescriptor(), m)));
  }

  /**
   * Returns a stream of parameterizations of the given type parameter found in a specific method
   * override (matching return type and parameter types).
   */
  private static Stream<TypeDescriptor> getParameterizationsInMethod(
      TypeVariable typeParameter,
      MethodDescriptor declarationMethodDescriptor,
      MethodDescriptor methodDescriptor) {
    return Stream.concat(
        Streams.zip(
                declarationMethodDescriptor.getParameterTypeDescriptors().stream(),
                methodDescriptor.getParameterTypeDescriptors().stream(),
                (declarationType, parameterizedType) ->
                    declarationType.getParameterizationsIn(typeParameter, parameterizedType))
            .flatMap(t -> t),
        declarationMethodDescriptor
            .getReturnTypeDescriptor()
            .getParameterizationsIn(typeParameter, methodDescriptor.getReturnTypeDescriptor()));
  }


}
