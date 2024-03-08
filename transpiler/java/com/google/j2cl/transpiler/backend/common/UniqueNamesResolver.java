/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler.backend.common;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Traverses a type and assigns non colliding variable names for type variables, locals and
 * parameters, avoiding collisions with names that are forbidden (keywords, JavaScript referenced
 * externs, module aliases, etc) and other variables accessible in the same member scope.
 */
public final class UniqueNamesResolver {

  /** Computes variable names that are contextually unique avoiding collisions. */
  public static Map<HasName, String> computeUniqueNames(
      Set<String> initiallyForbiddenNames, Type type) {
    final Set<String> forbiddenNames = new HashSet<>(initiallyForbiddenNames);

    // Gather variables by MemberDescriptor because clinit() and init() are synthesized at
    // generation from multiple members.
    final SetMultimap<MemberDescriptor, String> variableUniqueNamesByMember = HashMultimap.create();
    final Map<HasName, String> uniqueNameByVariable = new HashMap<>();

    // Collect type variables defined at the type level, and exclude their unique names from the
    // name pool to be used by local variables, parameters and type variables defined in methods.
    for (TypeVariable typeParameter : type.getDeclaration().getTypeParameterDescriptors()) {
      checkState(typeParameter.getNullabilityAnnotation() == NullabilityAnnotation.NONE);
      String uniqueName =
          computeUniqueName(typeParameter, Predicates.not(forbiddenNames::contains));
      forbiddenNames.add(uniqueName);
      uniqueNameByVariable.put(typeParameter, uniqueName);
    }

    // Create aliases for local variables, parameters and type variables defined in methods;
    // making sure that variables declared in the same member do not collide with reserved words,
    // aliases, type variables defined in the class or others in the same member.
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            registerUniqueNames(method.getDescriptor());
          }

          @Override
          public void exitFunctionExpression(FunctionExpression functionExpression) {
            registerUniqueNames(functionExpression.getDescriptor());
          }

          @Override
          public void exitNameDeclaration(NameDeclaration nameDeclaration) {
            registerUniqueName(getCurrentMember().getDescriptor(), nameDeclaration);
          }

          private void registerUniqueNames(MethodDescriptor methodDescriptor) {
            for (TypeVariable typeParameter : methodDescriptor.getTypeParameterTypeDescriptors()) {
              if (typeParameter.isWildcardOrCapture()) {
                continue;
              }
              String name = registerUniqueName(methodDescriptor, typeParameter);
              uniqueNameByVariable.put(typeParameter, name);
            }
          }

          @CanIgnoreReturnValue
          private String registerUniqueName(
              MemberDescriptor currentMemberDescriptor, HasName variable) {
            String uniqueName =
                uniqueNameByVariable.computeIfAbsent(
                    variable,
                    v ->
                        computeUniqueName(
                            v, name -> isNameAvailable(currentMemberDescriptor, name)));
            // Register the variable to the member descriptor, even if it already had a name,
            // that is because for some transformations, like devirtualization, type variables might
            // be moved from the enclosing class to the method.
            variableUniqueNamesByMember.put(currentMemberDescriptor, uniqueName);
            return uniqueName;
          }

          private boolean isNameAvailable(
              MemberDescriptor currentMemberDescriptor, String variableName) {
            return !forbiddenNames.contains(variableName)
                && !variableUniqueNamesByMember.containsEntry(
                    currentMemberDescriptor, variableName);
          }
        });
    return uniqueNameByVariable;
  }

  private static String computeUniqueName(HasName variable, Predicate<String> isNameAvailable) {
    String variableName = variable.getName();
    if (!isNameAvailable.apply(variableName)) {
      // When there are collisions synthesize a name of the style "name_<nnn>".
      variableName = variableName + "_";
      int suffix = 0;
      while (!isNameAvailable.apply((variableName + ++suffix))) {
        // ensure the alias does not collide with other local variables.
      }
      variableName += suffix;
    }
    return variableName;
  }

  private UniqueNamesResolver() {}
}
