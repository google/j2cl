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
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;

/**
 * Creates a default constructor for class that does not have any explicit constructors.
 *
 * <p>The default constructor has an empty parameter list and an empty body. Its visibility is the
 * same as the visibility of the class.
 */
public class CreateDefaultConstructors extends AbstractVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new CreateDefaultConstructors());
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
        AstUtils.createDefaultConstructorDescriptor(type.getDescriptor(), type.getVisibility());
    type.addMethod(0, Method.Builder.fromDefault().setMethodDescriptor(methodDescriptor).build());
  }
}
