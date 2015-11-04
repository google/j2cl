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

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traverses a JavaType and gathers variable names in each method and creates
 * non colliding local aliases for variables that collide with import module names or collide
 * with JavaScript keywords.
 */
public class VariableAliasesGatheringVisitor extends AbstractVisitor {
  private static final String VARIABLE_PREFIX = "l_";
  private List<String> importAliases = new ArrayList<>();
  private Multimap<Method, String> variableNamesByMethod = HashMultimap.create();
  private Map<Variable, String> aliasByVariable = new HashMap<>();

  /**
   * Visitor class that is used to collect all variable names in each method.
   */
  private class VariableNamesCollector extends AbstractVisitor {
    @Override
    public void exitVariable(Variable variable) {
      variableNamesByMethod.put(getCurrentMethod(), variable.getName());
    }
  }

  @Override
  public void exitVariable(Variable variable) {
    if (aliasByVariable.containsKey(variable)) {
      return;
    }
    String variableName = variable.getName();
    if (importAliases.contains(variableName) || JsProtectedNames.isKeyword(variableName)) {
      // add prefix "l_" to the local variable whose name collides with an import alias
      // or collides with a JavaScript keyword.
      variableName = VARIABLE_PREFIX + variableName;
      while (variableNamesByMethod.containsEntry(getCurrentMethod(), variableName)) {
        // add more prefix to ensure the alias does not collide with other local variables.
        variableName = VARIABLE_PREFIX + variableName;
      }
      aliasByVariable.put(variable, variableName);
    }
  }

  /**
   * Returns the aliases of the variables whose names collide with import aliases or Javascript
   * keywords.
   */
  public static Map<Variable, String> gatherVariableAliases(
      List<Import> imports, JavaType javaType) {
    return new VariableAliasesGatheringVisitor().doGatherVariableAliases(imports, javaType);
  }

  private Map<Variable, String> doGatherVariableAliases(List<Import> imports, JavaType javaType) {
    // get import aliases from the passing in Imports.
    getImportAliases(imports, javaType);

    // collect variable names in each method.
    javaType.accept(new VariableNamesCollector());

    // compute variable aliases.
    javaType.accept(this);

    return this.aliasByVariable;
  }

  private void getImportAliases(List<Import> imports, JavaType javaType) {
    this
        .importAliases.addAll(
            Lists.transform(
                imports,
                new Function<Import, String>() {
                  @Override
                  public String apply(Import importModule) {
                    return importModule.getAlias();
                  }
                }));
    this.importAliases.add(javaType.getDescriptor().getClassName());
  }
}
