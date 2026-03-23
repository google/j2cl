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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Collects unused imports, so they can be commented out. */
class UnusedImportsNodeCollector extends TreeScanner<Void, Void> {
  private final List<ImportTree> unusedImports = new ArrayList<>();
  private final Set<String> referencedNames = new HashSet<>();
  private final Set<Tree> nodesToRemove;

  public UnusedImportsNodeCollector(Set<Tree> nodesToRemove) {
    this.nodesToRemove = nodesToRemove;
  }

  @Override
  public Void scan(Tree tree, Void unused) {
    if (tree != null && nodesToRemove.contains(tree)) {
      return null; // Do not scan children of stripped nodes.
    }
    return super.scan(tree, null);
  }

  @Override
  public Void visitImport(ImportTree importTree, Void unused) {
    // Prevent visiting the names inside the import themselves.
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void unused) {
    referencedNames.add(node.getName().toString());
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree compilationUnitTree, Void unused) {
    if (compilationUnitTree.getPackage() != null) {
      scan(compilationUnitTree.getPackage().getAnnotations(), null);
    }
    super.visitCompilationUnit(compilationUnitTree, null);

    for (ImportTree importTree : compilationUnitTree.getImports()) {
      String importString = importTree.getQualifiedIdentifier().toString();
      if (importString.endsWith(".*")) {
        // Assume .* imports are always needed.
        continue;
      }

      String importedClass = importString.substring(importString.lastIndexOf('.') + 1);

      if (!referencedNames.contains(importedClass)) {
        unusedImports.add(importTree);
      }
    }
    return null;
  }

  public List<ImportTree> getUnusedImports() {
    return unusedImports;
  }
}
