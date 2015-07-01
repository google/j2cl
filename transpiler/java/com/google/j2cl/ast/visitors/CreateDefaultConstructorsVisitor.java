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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Ensures that all Types have a real materialized constructor and not just an implicit default
 * constructor.
 */
public class CreateDefaultConstructorsVisitor extends AbstractVisitor {
  public static void doCreateDefaultConstructors(CompilationUnit compilationUnit) {
    new CreateDefaultConstructorsVisitor().createDefaultConstructors(compilationUnit);
  }

  @Override
  public boolean enterJavaType(JavaType type) {
    if (type.isInterface()) {
      return false;
    }

    for (Method method : type.getMethods()) {
      if (method.isConstructor()) {
        // If there is any explicit constructor, then don't synthesize a default one.
        return false;
      }
    }

    synthesizeDefaultConstructor(type);
    return false;
  }

  private void createDefaultConstructors(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  private void synthesizeDefaultConstructor(JavaType type) {
    MethodReference methodReference =
        MethodReference.create(
            true,
            Visibility.PUBLIC,
            type.getSelfReference(),
            type.getSelfReference().getClassName(),
            true,
            type.getSelfReference());
    Block body = new Block(new ArrayList<Statement>());
    List<Variable> parameters = new ArrayList<>();
    Method defaultConstructor = new Method(methodReference, parameters, body);
    type.addMethod(defaultConstructor);
  }
}
