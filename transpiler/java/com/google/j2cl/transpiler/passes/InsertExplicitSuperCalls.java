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
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;

/**
 * Makes the implicit super call in a constructor explicit.
 *
 * <p>The implicit super call invokes the default constructor that has an empty parameter list.
 */
public class InsertExplicitSuperCalls extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (type.isInterface() || type.getSuperTypeDescriptor() == null) {
      return;
    }

    type.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            if (!method.isConstructor()) {
              return;
            }
            if (AstUtils.hasConstructorInvocation(method)) {
              return;
            }
            // Only inserts explicit super() call to a constructor that does not have
            // super() or this() call (provided that the type has a superclass).
            synthesizeSuperCall(method, getCurrentType());
          }
        });
  }

  private static void synthesizeSuperCall(Method constructor, Type type) {
    MethodDescriptor superConstructor = type.getImplicitSuperCallTarget();
    MethodCall superMethodCall =
        MethodCall.builderFrom(superConstructor)
            .setArguments(AstUtils.maybePackageVarargs(superConstructor, ImmutableList.of()))
            .build();
    constructor
        .getBody()
        .getStatements()
        .addFirst(
            AstUtils.resolveImplicitQualifier(superMethodCall, type.getTypeDescriptor())
                .makeStatement(constructor.getBody().getSourcePosition()));
  }
}
