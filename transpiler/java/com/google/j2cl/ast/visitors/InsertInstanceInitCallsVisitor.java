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

import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Insert instance $init call to each constructor.
 */
public class InsertInstanceInitCallsVisitor extends AbstractVisitor {
  public static void doInsertInstanceInitCall(CompilationUnit compilationUnit) {
    new InsertInstanceInitCallsVisitor().insertInstanceInitCall(compilationUnit);
  }

  @Override
  public boolean enterMethod(Method method) {
    if (!method.isConstructor() || ASTUtils.hasThisCall(method)) {
      // A constructor with this() call does not need $init call.
      return false;
    }
    synthesizeInstanceInitCall(method);
    return false;
  }

  private void insertInstanceInitCall(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  private void synthesizeInstanceInitCall(Method method) {
    MethodDescriptor initMethodDescriptor =
        ASTUtils.createInitMethodDescriptor(method.getDescriptor().getEnclosingClassDescriptor());

    List<Expression> arguments = new ArrayList<>();
    MethodCall initCall = new MethodCall(null, initMethodDescriptor, arguments);
    // If the constructor has a super() call, insert $init call after it. Otherwise, insert
    // to the top of the method body.
    int insertIndex = ASTUtils.hasSuperCall(method) ? 1 : 0;
    method.getBody().getStatements().add(insertIndex, new ExpressionStatement(initCall));
  }
}
