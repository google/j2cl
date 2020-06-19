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
package com.google.j2cl.tools.gwtincompatible;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

/** Collects unused imports, so they can be commented out, {@see GwtIncompatibleStripper}. */
class UnusedImportsNodeCollector extends ASTVisitor {
  private List<ImportDeclaration> unusedImports = new ArrayList<>();
  private Set<String> referencedNames = new HashSet<>();

  @Override
  public boolean visit(ImportDeclaration importDeclaration) {
    // Prevent visiting the names inside the import themselves.
    return false;
  }

  @Override
  public boolean visit(QualifiedName qualifiedName) {
    // We need the first component of the qualified name for example for Foo.Bar.baz
    // we are looking for the import of Foo.
    Name qualifier = qualifiedName.getQualifier();
    while (!qualifier.isSimpleName()) {
      qualifier = ((QualifiedName) qualifier).getQualifier();
    }
    referencedNames.add(qualifier.getFullyQualifiedName());
    return false;
  }

  @Override
  public boolean visit(SimpleName simpleName) {
    referencedNames.add(simpleName.getIdentifier());
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void endVisit(CompilationUnit compilationUnit) {
    List<ImportDeclaration> imports = compilationUnit.imports();
    for (ImportDeclaration importDeclaration : imports) {
      if (importDeclaration.isOnDemand()) {
        // Assume .* imports are always needed.
        continue;
      }
      SimpleName importedClass =
          importDeclaration.getName().isSimpleName()
              ? (SimpleName) importDeclaration.getName()
              : ((QualifiedName) importDeclaration.getName()).getName();

      if (!referencedNames.contains(importedClass.getIdentifier())) {
        unusedImports.add(importDeclaration);
      }
    }
  }

  public List<ImportDeclaration> getUnusedImports() {
    return unusedImports;
  }
}
