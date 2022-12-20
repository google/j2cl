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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A visitor that finds all the classes, methods and fields marked with a given annotation, e.g.
 * {@code GwtIncompatible}
 */
public final class AnnotatedNodeCollector {

  /**
   * Returns all the class, method or field nodes that are marked with a the given annotation. The
   * nodes are returned in order based on the position on the file and won't overlap.
   */
  public static Set<String> filesWithAnnotation(
      List<CompilationUnitTree> compilationUnits, String annotationName) {
    Set<String> filenames = new LinkedHashSet<>();
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      compilationUnit.accept(
          new TreeScanner<Void, Void>() {
            @Override
            public Void visitCompilationUnit(CompilationUnitTree unit, Void unused) {
              scan(unit.getTypeDecls(), null);
              return null;
            }

            @Override
            public Void visitAnnotatedType(AnnotatedTypeTree node, Void unused) {
              checkGwtIncompatibleAnnotations(node.getAnnotations());
              return null;
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
              checkGwtIncompatibleAnnotations(node.getModifiers().getAnnotations());
              return null;
            }

            @Override
            public Void visitMethod(MethodTree node, Void unused) {
              checkGwtIncompatibleAnnotations(node.getModifiers().getAnnotations());
              return null;
            }

            @Override
            public Void visitVariable(VariableTree node, Void unused) {
              checkGwtIncompatibleAnnotations(node.getModifiers().getAnnotations());
              return null;
            }

            private void checkGwtIncompatibleAnnotations(
                List<? extends AnnotationTree> annotations) {
              if (annotations.stream()
                  .anyMatch(a -> getLastComponent(a.getAnnotationType()).equals(annotationName))) {
                filenames.add(compilationUnit.getSourceFile().getName());
              }
            }
          },
          null);
    }
    return filenames;
  }

  private static String getLastComponent(Tree name) {
    switch (name.getKind()) {
      case IDENTIFIER:
        return ((IdentifierTree) name).getName().toString();
      case MEMBER_SELECT:
        return ((MemberSelectTree) name).getIdentifier().toString();
      default:
        return "";
    }
  }

  private AnnotatedNodeCollector() {}
}
