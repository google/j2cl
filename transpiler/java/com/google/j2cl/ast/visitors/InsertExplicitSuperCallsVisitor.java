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
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Make the implicit super call in a constructor explicit.
 */
public class InsertExplicitSuperCallsVisitor extends AbstractVisitor {
  public static void doInsertExplicitSuperCalls(CompilationUnit compilationUnit) {
    new InsertExplicitSuperCallsVisitor().insertExplicitSuperCalls(compilationUnit);
  }

  private JavaType currentJavaType;

  @Override
  public boolean enterJavaType(JavaType type) {
    if (type.isInterface()) {
      return false;
    }
    currentJavaType = type;
    return true;
  }

  @Override
  public boolean enterMethod(Method method) {
    /*
     * Only inserts explicit super() call to a constructor that does not have
     * a super() or this() call, and the corresponding type does have a super class.
     */
    if (!method.isConstructor()
        || ASTUtils.hasConstructorInvocation(method)
        || currentJavaType.getSuperTypeRef() == null) {
      return false;
    }
    synthesizeSuperCall(method, currentJavaType.getSuperTypeRef());
    return false;
  }

  @Override
  public void exitJavaType(JavaType type) {
    currentJavaType = null;
  }

  private void insertExplicitSuperCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  private void synthesizeSuperCall(Method method, TypeReference superTypeRef) {
    MethodReference methodRef =
        MethodReference.create(
            false,
            Visibility.PUBLIC,
            superTypeRef,
            superTypeRef.getClassName(),
            true,
            superTypeRef);
    List<Expression> arguments = new ArrayList<>();
    MethodCall superCall = new MethodCall(null, methodRef, arguments);
    method.getBody().getStatements().add(0, new ExpressionStatement(superCall));
  }
}
