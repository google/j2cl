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
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a default constructor for class that does not have any explicit constructors.
 *
 * <p>The default constructor has an empty parameter list and an empty body. Its visibility is the
 * same as the visibility of the class.
 */
public class CreateDefaultConstructorsVisitor extends AbstractVisitor {
  public static void applyTo(CompilationUnit compilationUnit) {
    new CreateDefaultConstructorsVisitor().createDefaultConstructors(compilationUnit);
  }

  private void createDefaultConstructors(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
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

  private void synthesizeDefaultConstructor(JavaType type) {
    MethodDescriptor methodDescriptor =
        AstUtils.createConstructorDescriptor(type.getDescriptor(), type.getVisibility());
    Block body = new Block(new ArrayList<Statement>());
    List<Variable> parameters = new ArrayList<>();
    Method defaultConstructor = new Method(methodDescriptor, parameters, body);
    type.addMethod(0, defaultConstructor);
  }
}
