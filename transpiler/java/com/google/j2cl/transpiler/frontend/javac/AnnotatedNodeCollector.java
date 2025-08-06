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
package com.google.j2cl.transpiler.frontend.javac;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.List;

/**
 * A visitor that finds all the classes, methods and fields marked with the given annotation(s),
 * e.g. {@code GwtIncompatible}
 */
public final class AnnotatedNodeCollector extends TreeScanner<Void, Void> {

  private final ImmutableList<String> annotationNames;
  private final boolean stopTraversalOnMatch;
  private final Multimap<String, Tree> nodes;

  public AnnotatedNodeCollector(
      ImmutableList<String> annotationNames, boolean stopTraversalOnMatch) {
    this.annotationNames = annotationNames;
    this.stopTraversalOnMatch = stopTraversalOnMatch;
    nodes = MultimapBuilder.hashKeys(annotationNames.size()).arrayListValues().build();
  }

  public ImmutableSet<Tree> getNodes() {
    return ImmutableSet.copyOf(nodes.values());
  }

  public ImmutableSet<Tree> getNodesWithAnnotation(String annotationName) {
    return ImmutableSet.copyOf(nodes.get(annotationName));
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree unit, Void unused) {
    return scan(unit.getTypeDecls(), null);
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree node, Void unused) {
    boolean foundMatch = checkForMatchingAnnotations(node, node.getAnnotations());
    if (stopTraversalOnMatch && foundMatch) {
      return null;
    }
    return super.visitAnnotatedType(node, null);
  }

  @Override
  public Void visitClass(ClassTree node, Void unused) {
    boolean foundMatch = checkForMatchingAnnotations(node, node.getModifiers().getAnnotations());
    if (stopTraversalOnMatch && foundMatch) {
      return null;
    }
    return super.visitClass(node, null);
  }

  @Override
  public Void visitMethod(MethodTree node, Void unused) {
    boolean foundMatch = checkForMatchingAnnotations(node, node.getModifiers().getAnnotations());
    if (stopTraversalOnMatch && foundMatch) {
      return null;
    }
    return super.visitMethod(node, null);
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    boolean foundMatch = checkForMatchingAnnotations(node, node.getModifiers().getAnnotations());
    if (stopTraversalOnMatch && foundMatch) {
      return null;
    }
    return super.visitVariable(node, null);
  }

  private boolean checkForMatchingAnnotations(
      Tree node, List<? extends AnnotationTree> annotations) {
    boolean foundMatch = false;
    for (var annotation : annotations) {
      String annotationName = getLastComponent(annotation.getAnnotationType());
      if (annotationNames.contains(annotationName)) {
        nodes.put(annotationName, node);
        foundMatch = true;
      }
    }
    return foundMatch;
  }

  private static String getLastComponent(Tree name) {
    return switch (name.getKind()) {
      case IDENTIFIER -> ((IdentifierTree) name).getName().toString();
      case MEMBER_SELECT -> ((MemberSelectTree) name).getIdentifier().toString();
      default -> "";
    };
  }
}
