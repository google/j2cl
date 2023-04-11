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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Contains aliases for variables and Type Descriptors. */
public class ClosureGenerationEnvironment {
  /**
   * A map from type binary named (e.g. a.b.Foo) to alias for that type. Keyed by binary name so
   * generic and non-generic permutations of a class all map to the same type.
   */
  private final Map<String, String> aliasByTypeBinaryName = new HashMap<>();

  private final Map<HasName, String> uniqueNameByVariable;

  private final ClosureTypesGenerator closureTypesGenerator = new ClosureTypesGenerator(this);

  public ClosureGenerationEnvironment(
      Collection<Import> imports, Map<HasName, String> uniqueNameByVariable) {
    for (Import anImport : imports) {
      String alias = anImport.getAlias();
      checkArgument(alias != null && !alias.isEmpty(), "Bad alias for %s", anImport.getElement());
      aliasByTypeBinaryName.put(anImport.getElement().getQualifiedBinaryName(), alias);
    }
    this.uniqueNameByVariable = uniqueNameByVariable;
  }

  public String getUniqueNameForVariable(HasName variable) {
    if (uniqueNameByVariable.containsKey(variable)) {
      return uniqueNameByVariable.get(variable);
    }
    return variable.getName();
  }

  public String aliasForType(DeclaredTypeDescriptor typeDescriptor) {
    return aliasForType(typeDescriptor.getTypeDeclaration());
  }

  public String aliasForType(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isExtern()) {
      return typeDeclaration.getQualifiedJsName();
    }
    String moduleAlias =
        aliasByTypeBinaryName.get(typeDeclaration.getEnclosingModule().getQualifiedBinaryName());
    checkState(
        moduleAlias != null, "An alias was needed for %s but no alias was found.", typeDeclaration);

    String innerTypeQualifier = typeDeclaration.getInnerTypeQualifier();

    return innerTypeQualifier.isEmpty() ? moduleAlias : moduleAlias + "." + innerTypeQualifier;
  }

  public String getClosureTypeString(TypeDescriptor typeDescriptor) {
    return closureTypesGenerator.getClosureTypeString(typeDescriptor);
  }

  public String getJsDocForParameter(MethodLike methodLike, int index) {
    return closureTypesGenerator.getJsDocForParameter(methodLike, index);
  }
}
