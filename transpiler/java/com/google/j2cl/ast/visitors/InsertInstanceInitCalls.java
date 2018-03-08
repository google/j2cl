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
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.common.SourcePosition;

/** Insert instance $init call to each constructor. */
public class InsertInstanceInitCalls extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      if (type.getInstanceInitializerBlocks().isEmpty()) {
        continue;
      }
      for (Method constructor : type.getConstructors()) {
        if (AstUtils.hasThisCall(constructor)) {
          // A constructor with this() call does not need $init call.
          continue;
        }
        synthesizeInstanceInitCall(constructor);
      }
    }
  }

  private static void synthesizeInstanceInitCall(Method constructor) {
    MethodDescriptor initMethodDescriptor =
        AstUtils.getInitMethodDescriptor(constructor.getDescriptor().getEnclosingTypeDescriptor());

    SourcePosition sourcePosition = constructor.getBody().getSourcePosition();

    // If the constructor has a super() call, insert $init call after it. Otherwise, insert
    // to the top of the method body.
    int insertIndex = AstUtils.hasSuperCall(constructor) ? 1 : 0;
    constructor
        .getBody()
        .getStatements()
        .add(
            insertIndex,
            MethodCall.Builder.from(initMethodDescriptor).build().makeStatement(sourcePosition));
  }
}

