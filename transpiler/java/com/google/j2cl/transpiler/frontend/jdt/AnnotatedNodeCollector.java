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
package com.google.j2cl.transpiler.frontend.jdt;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * A visitor that finds all the classes, methods and fields marked with a given annotation, e.g.
 * {@code GwtIncompatible}.
 */
public class AnnotatedNodeCollector extends ASTVisitor {
  private final List<ASTNode> nodes = new ArrayList<>();
  private final String annotationName;

  public AnnotatedNodeCollector(String annotationName) {
    this.annotationName = annotationName;
  }

  @Override
  public boolean visit(TypeDeclaration typeDeclaration) {
    return visitBodyDeclaration(typeDeclaration);
  }

  @Override
  public boolean visit(MethodDeclaration methodDeclaration) {
    return visitBodyDeclaration(methodDeclaration);
  }

  @Override
  public boolean visit(FieldDeclaration fieldDeclaration) {
    return visitBodyDeclaration(fieldDeclaration);
  }

  @Override
  public boolean visit(EnumConstantDeclaration enumConstantDeclaration) {
    return visitBodyDeclaration(enumConstantDeclaration);
  }

  /**
   * Returns all the class, method or field nodes that are marked with the annotation provided in
   * the constructor. The nodes are returned in order based on the position on the file and won't
   * overlap.
   */
  public List<ASTNode> getNodes() {
    return nodes;
  }

  private boolean visitBodyDeclaration(BodyDeclaration bodyDeclaration) {
    if (hasGwtIncompatibleAnnotation(bodyDeclaration, annotationName)) {
      nodes.add(bodyDeclaration);
      return false;
    }
    return true;
  }

  private static boolean hasGwtIncompatibleAnnotation(
      BodyDeclaration declaration, String annotationName) {
    for (Object modifier : declaration.modifiers()) {
      if (modifier instanceof Annotation) {
        Name name = ((Annotation) modifier).getTypeName();
        // Get the name of the class without package, since the {@code GwtIncompatible} annotation
        // can be defined anywhere.
        String simpleName =
            name.isSimpleName() ? name.toString() : ((QualifiedName) name).getName().toString();
        if (simpleName.equals(annotationName)) {
          return true;
        }
      }
    }
    return false;
  }
}
