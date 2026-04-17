/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Promotes mutability for base collection types in J2KT by replacing calls to mutation methods with
 * calls to the corresponding asMutable...() method.
 */
public class PromoteMutability extends AbstractJ2ktNormalizationPass {

  private final TypeDescriptors types = TypeDescriptors.get();

  private final ImmutableSet<MethodDescriptor> mutationMethods;
  private final ImmutableMap<TypeDeclaration, MethodDescriptor> asMutableMethodsByTypeDeclaration;

  public PromoteMutability() {
    ImmutableSet.Builder<MethodDescriptor> mutationMethodsBuilder = ImmutableSet.builder();
    ImmutableMap.Builder<TypeDeclaration, MethodDescriptor> asMutableMethodsBuilder =
        ImmutableMap.builder();

    // Collection
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilCollection,
        "asMutableCollection",
        "add",
        "addAll",
        "remove",
        "removeAll",
        "removeIf",
        "retainAll",
        "clear");

    // List
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilList,
        "asMutableList",
        "add",
        "addAll",
        "set",
        "sort",
        "remove",
        "removeFirst",
        "removeLast",
        "addFirst",
        "addLast");

    // Set
    addMutatingMethods(
        asMutableMethodsBuilder, mutationMethodsBuilder, types.javaUtilSet, "asMutableSet");

    // Map
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilMap,
        "asMutableMap",
        "put",
        "putAll",
        "putIfAbsent",
        "remove",
        "replace",
        "replaceAll",
        "clear",
        "compute",
        "computeIfAbsent",
        "computeIfPresent",
        "merge");

    // Iterator
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilIterator,
        "asMutableIterator",
        "remove");

    // ListIterator
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilListIterator,
        "asMutableListIterator",
        "add",
        "set");

    // Map.Entry
    addMutatingMethods(
        asMutableMethodsBuilder,
        mutationMethodsBuilder,
        types.javaUtilMapEntry,
        "asMutableEntry",
        "setValue");

    this.mutationMethods = mutationMethodsBuilder.build();
    this.asMutableMethodsByTypeDeclaration = asMutableMethodsBuilder.buildOrThrow();
  }

  private static void addMutatingMethods(
      ImmutableMap.Builder<TypeDeclaration, MethodDescriptor> asMutableMethodsBuilder,
      ImmutableSet.Builder<MethodDescriptor> mutationMethodsBuilder,
      DeclaredTypeDescriptor type,
      String asMutableMethodName,
      String... mutatingMethodNames) {
    MethodDescriptor asMutableMethod = type.getMethodDescriptorByName(asMutableMethodName);
    asMutableMethodsBuilder.put(type.getTypeDeclaration(), asMutableMethod);
    ImmutableSet<String> names = ImmutableSet.copyOf(mutatingMethodNames);

    for (MethodDescriptor method : type.getTypeDeclaration().getDeclaredMethodDescriptors()) {
      if (names.contains(method.getName())) {
        mutationMethodsBuilder.add(method.getDeclarationDescriptor());
      }
    }
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    rewriteMutationCalls(compilationUnit);
  }

  private void rewriteMutationCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor targetMethod = methodCall.getTarget();
            if (!isCollectionMutationMethod(targetMethod)) {
              return methodCall;
            }

            TypeDescriptor qualifierType = methodCall.getQualifier().getTypeDescriptor();
            MethodDescriptor asMutableMethod = getAsMutableMethodIfReadonly(qualifierType);
            if (asMutableMethod == null) {
              // Qualifier type was already mutable, no need to insert asMutable() call.
              return methodCall;
            }

            Map<TypeVariable, TypeDescriptor> parameterization =
                targetMethod.getEnclosingTypeDescriptor().getParameterization();

            asMutableMethod = asMutableMethod.specializeTypeVariables(parameterization);

            MethodCall asMutableCall =
                MethodCall.Builder.from(asMutableMethod)
                    .setQualifier(methodCall.getQualifier())
                    .build();

            return MethodCall.Builder.from(methodCall).setQualifier(asMutableCall).build();
          }
        });
  }

  /**
   * Returns whether a collection type that would normally become readonly in Kotlin has to be
   * mutable due to the use of lower-bounded type parameters (the "List<in T> is not legal in
   * Kotlin" problem)
   */
  private boolean isMutableDueToVariance(TypeDescriptor typeDescriptor) {
    if (typeDescriptor == null) {
      return false;
    }

    if (typeDescriptor instanceof DeclaredTypeDescriptor declaredTypeDescriptor) {
      if (!asMutableMethodsByTypeDeclaration.containsKey(
          declaredTypeDescriptor.getTypeDeclaration())) {
        // Not a base collection interface (non-collection type or a collection subtype which is
        // always mutable), no need to worry about variance-based mutability
        return false;
      }

      ImmutableList<TypeDescriptor> typeArguments =
          declaredTypeDescriptor.getTypeArgumentDescriptors();
      if (typeArguments.isEmpty()) {
        return false;
      }
      // TODO - b/502503059: Annotate type parameters with @KtOut and remove index-based logic.
      // In Kotlin, all type parameters of the readonly collection interfaces have declaration-site
      // covariance (except the first ("Key") type parameter of Map). That means they cannot be
      // instantiated with a lower type-bound (=contravariant). So the Kotlin backend will never
      // render collection interface types with lower bounds as readonly, but always as mutable.
      int startIndex = declaredTypeDescriptor.isSameBaseType(types.javaUtilMap) ? 1 : 0;
      for (int i = startIndex; i < typeArguments.size(); i++) {
        TypeDescriptor arg = typeArguments.get(i);
        if (arg instanceof TypeVariable typeVariable
            && typeVariable.getLowerBoundTypeDescriptor() != null) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns whether the given method is a mutation method. i.e. cannot be called on readonly
   * collection types without first calling a converter function like asMutableList.
   */
  private boolean isCollectionMutationMethod(MethodDescriptor methodDescriptor) {
    if (mutationMethods.contains(methodDescriptor.getDeclarationDescriptor())) {
      return true;
    }
    return methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
        .map(MethodDescriptor::getDeclarationDescriptor)
        .anyMatch(mutationMethods::contains);
  }

  /**
   * Returns the asMutable() method for a type if it is a collection base interface that will be
   * translated as a readonly Kotlin type, null otherwise.
   */
  @Nullable
  private MethodDescriptor getAsMutableMethodIfReadonly(TypeDescriptor type) {
    while (type instanceof TypeVariable typeVariable) {
      // Treat `T extends List<...>` like `List<...>`. Other type variables are not considered
      // readonly.
      type = typeVariable.getUpperBoundTypeDescriptor();
      if (type == null) {
        return null;
      }
    }
    if (isMutableDueToVariance(type)) {
      // The type is not readonly due to contravariance (List<in T> does not exist in Kotlin so
      // the transpiler backend will render the type as mutable).
      return null;
    }
    if (type instanceof DeclaredTypeDescriptor declaredTypeDescriptor) {
      return asMutableMethodsByTypeDeclaration.get(declaredTypeDescriptor.getTypeDeclaration());
    }
    return null;
  }
}
