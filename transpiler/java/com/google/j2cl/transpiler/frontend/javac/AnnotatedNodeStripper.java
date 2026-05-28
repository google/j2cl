/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Name;

/** Strips nodes annotated with specific annotations. */
public class AnnotatedNodeStripper extends TreeScanner<Void, Void> {

  /**
   * Strips nodes annotated with specific annotations from the given compilation unit.
   *
   * @return true if any changes were made to the compilation unit.
   */
  @CanIgnoreReturnValue
  public static boolean strip(JCCompilationUnit cu, Collection<String> annotationsToStrip) {
    // Strip annotated nodes.
    var stripper = new AnnotatedNodeStripper(annotationsToStrip);
    stripper.scan(cu, null);
    return stripper.madeChanges;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
    JCCompilationUnit cu = (JCCompilationUnit) node;

    // 1. Remove the top level incompatible declarations so that they and their child nodes are not
    // processed.
    cu.defs = filterIncompatible(cu.defs);

    // 2. Traverse the rest of the nodes in the compilation units to collect referenced names and
    // remove incompatible nodes.
    super.visitCompilationUnit(cu, null);

    if (!madeChanges) {
      // Don't remove unused imports if no changes were made to the compilation unit.
      return null;
    }

    // 3. Remove unused imports.
    removeUnusedImports(cu);
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, Void unused) {
    JCClassDecl classDecl = (JCClassDecl) node;
    checkState(!isIncompatible(classDecl));

    classDecl.defs = filterIncompatible(classDecl.defs);
    return super.visitClass(node, null);
  }

  @Override
  public Void visitMethod(MethodTree node, Void unused) {
    JCMethodDecl methodDecl = (JCMethodDecl) node;
    // We should never traverse an incompatible method.
    checkState(!isIncompatible(methodDecl));
    return super.visitMethod(node, null);
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    JCVariableDecl varDecl = (JCVariableDecl) node;
    // We should never traverse an incompatible field.
    checkState(!isIncompatible(varDecl));
    return super.visitVariable(node, null);
  }

  @Override
  public Void visitImport(ImportTree importTree, Void unused) {
    // Prevent visiting the names inside the import themselves.
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void unused) {
    // Collect referenced names.
    referencedNames.add(node.getName());
    return null;
  }

  private List<JCTree> filterIncompatible(List<JCTree> defs) {
    var filtered = defs.stream().filter(Predicates.not(this::isIncompatible)).toList();
    if (filtered.size() != defs.size()) {
      madeChanges = true;
      return List.from(filtered);
    }
    return defs;
  }

  private boolean isIncompatible(JCTree def) {
    return switch (def.getTag()) {
      case CLASSDEF -> hasIncompatibleAnnotation(((JCClassDecl) def).mods.annotations);
      case METHODDEF -> hasIncompatibleAnnotation(((JCMethodDecl) def).mods.annotations);
      case VARDEF -> hasIncompatibleAnnotation(((JCVariableDecl) def).mods.annotations);
      default -> false;
    };
  }

  private boolean hasIncompatibleAnnotation(List<JCAnnotation> annotations) {
    return annotations != null
        && annotations.stream()
            .map(a -> getSimpleName(a.annotationType).toString())
            .anyMatch(annotationsToStrip::contains);
  }

  private void removeUnusedImports(JCCompilationUnit cu) {
    var unusedImports = new HashSet<ImportTree>();
    for (ImportTree importTree : cu.getImports()) {
      String importString = importTree.getQualifiedIdentifier().toString();
      if (importString.endsWith(".*")) {
        // Assume .* imports are always needed.
        continue;
      }

      if (!referencedNames.contains(getSimpleName(importTree.getQualifiedIdentifier()))) {
        unusedImports.add(importTree);
      }
    }
    cu.defs = List.from(cu.defs.stream().filter(Predicates.not(unusedImports::contains)).toList());
  }

  private static Name getSimpleName(Tree name) {
    return switch (name.getKind()) {
      case IDENTIFIER -> ((IdentifierTree) name).getName();
      case MEMBER_SELECT -> ((MemberSelectTree) name).getIdentifier();
      default -> throw new IllegalArgumentException("Unexpected name kind: " + name.getKind());
    };
  }

  private final Set<String> annotationsToStrip;
  private final Set<Name> referencedNames = new HashSet<>();
  private boolean madeChanges = false;

  private AnnotatedNodeStripper(Collection<String> annotationsToStrip) {
    this.annotationsToStrip = ImmutableSet.copyOf(annotationsToStrip);
  }
}
