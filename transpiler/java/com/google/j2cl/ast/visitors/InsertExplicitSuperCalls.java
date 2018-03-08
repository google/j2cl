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

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Type;

/**
 * Makes the implicit super call in a constructor explicit.
 *
 * <p>The implicit super call invokes the default constructor that has an empty parameter list.
 */
public class InsertExplicitSuperCalls extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      if (type.isInterface() || type.getSuperTypeDescriptor() == null) {
        continue;
      }
      for (Method constructor : type.getConstructors()) {
        if (AstUtils.hasConstructorInvocation(constructor)) {
          continue;
        }
        // Only inserts explicit super() call to a constructor that does not have
        // super() or this() call (provided that the type has a superclass).
        synthesizeSuperCall(constructor);
      }
    }
  }

  private static void synthesizeSuperCall(Method constructor) {
    DeclaredTypeDescriptor superTypeDescriptor =
        constructor.getDescriptor().getEnclosingTypeDescriptor().getSuperTypeDescriptor();

    constructor
        .getBody()
        .getStatements()
        .add(
            0,
            MethodCall.Builder.from(
                    AstUtils.createImplicitConstructorDescriptor(superTypeDescriptor))
                .build()
                .makeStatement(constructor.getBody().getSourcePosition()));
  }
}
