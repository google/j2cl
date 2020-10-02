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
package com.google.j2cl.transpiler.backend.closure;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traverses a type and assigns non colliding variable names for type variables, locals and
 * parameters, avoiding collisions with keywords, referenced externs, module aliases and other
 * variables accessible in the same member scope.
 */
class UniqueVariableNamesGatherer extends AbstractVisitor {

  /** Computes variable names that are contextually unique avoiding collisions. */
  public static Map<HasName, String> computeUniqueVariableNames(
      Iterable<Import> imports, Type type) {
    final Set<String> forbiddenNames =
        Streams.stream(imports)
            // Take the first component of the alias. Most aliases are not qualified names, but
            // externs might be qualified names and only the first component needs to be considered
            // to avoid top level name clashes.
            .map(anImport -> anImport.getAlias().split("\\\\.")[0])
            .collect(Collectors.toSet());

    // Gather variables by MemberDescriptor because clinit() and init() are synthesized at
    // generation from multiple members.
    final Multimap<MemberDescriptor, String> variableUniqueNamesByMember = HashMultimap.create();
    final Map<HasName, String> uniqueNameByVariable = new HashMap<>();

    // Collect type variables defined at the type level, and exclude their unique names from the
    // name pool to be used by local variables, parameters and type variables defined in methods.
    for (TypeVariable typeVariable : type.getDeclaration().getTypeParameterDescriptors()) {
      String uniqueName = computeUniqueName(typeVariable, Predicates.not(forbiddenNames::contains));
      forbiddenNames.add(uniqueName);
      uniqueNameByVariable.put(typeVariable, uniqueName);
    }

    // Create aliases for local variables, parameters and type variables defined in methods;
    // making sure that variables declared in the same member do not collide with reserved words,
    // aliases, type variables defined in the class or others in the same member.
    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            for (TypeVariable typeVariable :
                method.getDescriptor().getTypeParameterTypeDescriptors()) {
              if (typeVariable.isWildcardOrCapture()) {
                continue;
              }
              registerUniqueName(method.getDescriptor(), typeVariable);
            }
          }

          @Override
          public void exitVariable(Variable variable) {
            registerUniqueName(getCurrentMember().getDescriptor(), variable);
          }

          private void registerUniqueName(
              MemberDescriptor currentMemberDescriptor, HasName variable) {
            uniqueNameByVariable.computeIfAbsent(
                variable,
                v -> {
                  String uniqueName =
                      computeUniqueName(v, name -> isNameAvailable(currentMemberDescriptor, name));
                  variableUniqueNamesByMember.put(currentMemberDescriptor, uniqueName);
                  return uniqueName;
                });
          }

          private boolean isNameAvailable(
              MemberDescriptor currentMemberDescriptor, String variableName) {
            return JsProtectedNames.isLegalName(variableName)
                && !forbiddenNames.contains(variableName)
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
}
