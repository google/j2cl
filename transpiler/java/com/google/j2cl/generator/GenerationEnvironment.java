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
package com.google.j2cl.generator;

import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.generator.visitors.Import;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains alias for variables and Type Descriptors.
 */
public class GenerationEnvironment {
  private Map<TypeDescriptor, String> aliasByTypeDescriptor = new HashMap<>();
  private final Map<Variable, String> aliasByVariable;
  private TypeDescriptor clinitEnclosingTypeDescriptor;

  public GenerationEnvironment(Collection<Import> imports, Map<Variable, String> aliasByVariable) {
    JsDocNameUtils.init();
    for (Import anImport : imports) {
      aliasByTypeDescriptor.put(anImport.getTypeDescriptor(), anImport.getAlias());
    }
    this.aliasByVariable = aliasByVariable;
  }

  public String aliasForVariable(Variable variable) {
    if (aliasByVariable.containsKey(variable)) {
      return aliasByVariable.get(variable);
    }
    return variable.getName();
  }

  public String aliasForType(TypeDescriptor typeDescriptor) {
    String alias = aliasByTypeDescriptor.get(typeDescriptor);
    return alias == null ? typeDescriptor.getClassName() : alias;
  }

  public TypeDescriptor getClinitEnclosingTypeDescriptor() {
    return clinitEnclosingTypeDescriptor;
  }

  /**
   * Allows the template system to control whether static field references are generated to the
   * wrapping getter/setter or the private data field. Makes it possible to ensure that references
   * are direct when inside of $clinit() methods.
   */
  public void setClinitEnclosingTypeDescriptor(TypeDescriptor clinitEnclosingTypeDescriptor) {
    this.clinitEnclosingTypeDescriptor = clinitEnclosingTypeDescriptor;
  }
}
