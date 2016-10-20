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
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import java.util.ArrayList;
import java.util.List;

/** Insert instance $init call to each constructor. */
public class InsertInstanceInitCalls extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Visitor());
  }

  private static class Visitor extends AbstractVisitor {
    @Override
    public boolean enterMethod(Method method) {
      if (!method.isConstructor() || AstUtils.hasThisCall(method)) {
        // A constructor with this() call does not need $init call.
        return false;
      }
      synthesizeInstanceInitCall(method);
      return false;
    }

    private void synthesizeInstanceInitCall(Method method) {
      MethodDescriptor initMethodDescriptor =
          AstUtils.createInitMethodDescriptor(
              method.getDescriptor().getEnclosingClassTypeDescriptor());

      List<Expression> arguments = new ArrayList<>();
      MethodCall initCall =
          MethodCall.Builder.from(initMethodDescriptor).setArguments(arguments).build();
      // If the constructor has a super() call, insert $init call after it. Otherwise, insert
      // to the top of the method body.
      int insertIndex = AstUtils.hasSuperCall(method) ? 1 : 0;
      method.getBody().getStatements().add(insertIndex, initCall.makeStatement());
    }
  }
}
