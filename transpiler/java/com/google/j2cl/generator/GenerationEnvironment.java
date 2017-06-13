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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.generator.visitors.Import;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Contains aliases for variables and Type Descriptors. */
public class GenerationEnvironment {
  /**
   * A map from type binary named (e.g. a.b.Foo) to alias for that type. Keyed by binary name so
   * generic and non-generic permutations of a class all map to the same type.
   */
  private Map<String, String> aliasByTypeBinaryName = new HashMap<>();

  private final Map<Variable, String> aliasByVariable;
  private TypeDescriptor enclosingTypeDescriptor;

  public GenerationEnvironment(Collection<Import> imports, Map<Variable, String> aliasByVariable) {
    for (Import anImport : imports) {
      String alias = anImport.getAlias();
      checkArgument(alias != null && !alias.isEmpty(), "Bad alias for %s", anImport.getElement());
      aliasByTypeBinaryName.put(anImport.getElement().getQualifiedBinaryName(), alias);
    }
    this.aliasByVariable = aliasByVariable;
  }

  public String aliasForVariable(Variable variable) {
    if (aliasByVariable.containsKey(variable)) {
      return aliasByVariable.get(variable);
    }
    return variable.getName();
  }

  public String aliasForType(TypeDeclaration typeDeclaration) {
    String alias = aliasByTypeBinaryName.get(typeDeclaration.getQualifiedBinaryName());
    checkState(
        alias != null, "An alias was needed for %s but no alias was found.", typeDeclaration);
    return alias;
  }

  public String aliasForType(TypeDescriptor typeDescriptor) {
    checkArgument(!typeDescriptor.isTypeVariable());
    checkArgument(!typeDescriptor.isWildCardOrCapture());
    if (typeDescriptor.isUnion()) {
      return "("
          + typeDescriptor
              .getUnionedTypeDescriptors()
              .stream()
              .map(this::aliasForType)
              .collect(Collectors.joining(" | "))
          + ")";
    }

    return aliasForType(typeDescriptor.getTypeDeclaration());
  }

  public TypeDescriptor getEnclosingTypeDescriptor() {
    return enclosingTypeDescriptor;
  }

  /**
   * Allows the template system to control whether static field references are generated to the
   * wrapping getter/setter or the private data field. Makes it possible to ensure that references
   * are direct when the reference site is internal to the same class..
   */
  public void setEnclosingTypeDescriptor(TypeDescriptor enclosingTypeDescriptor) {
    this.enclosingTypeDescriptor = enclosingTypeDescriptor;
  }
}
