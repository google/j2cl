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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.VariableReference;

/**
 * Rewrites System.getProperty to hook into goog.define.
 */
public class RewriteSystemGetPropertyMethod extends AbstractRewriter {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new RewriteSystemGetPropertyMethod());
  }

  @Override
  public Node rewriteMethod(Method method) {
    if (!"java.lang.System"
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor().getSourceName())
        || !"getProperty".equals(method.getDescriptor().getMethodName())) {
      return method;
    }

    Expression returnValueExpression = NullLiteral.NULL;
    if (method.getParameters().size() == 2) {
      returnValueExpression = new VariableReference(method.getParameters().get(1));
    }

    return Method.Builder.from(method)
        .clearStatements()
        // TODO: integrate closures goog.define here.
        .addStatements(
            new ReturnStatement(returnValueExpression, TypeDescriptors.get().javaLangString))
        .build();
  }
}
