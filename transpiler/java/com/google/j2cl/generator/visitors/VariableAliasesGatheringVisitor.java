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
package com.google.j2cl.generator.visitors;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Traverses a Type and gathers variable names in each method and creates non colliding local
 * aliases for variables that collide with import module names or collide with JavaScript keywords.
 */
public class VariableAliasesGatheringVisitor extends AbstractVisitor {
  private static final String VARIABLE_PREFIX = "l_";

  /**
   * Returns the aliases of the variables whose names collide with import aliases or Javascript
   * keywords.
   */
  public static Map<Variable, String> gatherVariableAliases(Iterable<Import> imports, Type type) {
    final Set<String> forbiddenNames =
        Streams.stream(imports)
            .map(
                anImport -> {
                  if (!anImport.getElement().isExtern()) {
                    return anImport.getAlias();
                  }
                  // Collect the top level name for the extern.
                  return anImport.getElement().getQualifiedName().split("\\\\.")[0];
                })
            .collect(toImmutableSet());
    final Multimap<Member, String> variableNamesByMember = HashMultimap.create();
    final Map<Variable, String> aliasByVariable = new HashMap<>();

    class VariableGatherer extends AbstractVisitor {
      @Override
      public void exitVariable(Variable variable) {
        variableNamesByMember.put(getCurrentMember(), variable.getName());
      }
    }

    class VariableRenamingVisitor extends AbstractVisitor {
      @Override
      public void exitVariable(Variable variable) {
        if (aliasByVariable.containsKey(variable) || variable.isRaw()) {
          return;
        }
        String variableName = variable.getName();
        if (isNameForbidden(variableName)) {
          // add prefix "l_" to the local variable whose name collides with an import alias
          // or collides with a JavaScript keyword.
          variableName = VARIABLE_PREFIX + variableName;
          int suffix = 0;
          while (isNameForbidden(variableName)
              || variableNamesByMember.containsEntry(getCurrentMember(), variableName)) {
            // add more prefix to ensure the alias does not collide with other local variables.
            variableName = VARIABLE_PREFIX + variableName + suffix++;
          }
          aliasByVariable.put(variable, variableName);
        }
      }

      private boolean isNameForbidden(String variableName) {
        return forbiddenNames.contains(variableName) || !JsProtectedNames.isLegalName(variableName);
      }
    }
    type.accept(new VariableGatherer());
    type.accept(new VariableRenamingVisitor());
    return aliasByVariable;
  }
}
