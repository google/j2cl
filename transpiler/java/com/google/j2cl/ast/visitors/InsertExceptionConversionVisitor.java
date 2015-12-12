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
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;

/**
 * Adds Java Throwable to JavaScript Error conversion.
 * <p> This pass makes sure that exceptions work well across Java/JavaScript boundary.
 * Instead of throwing subclasses of Java Throwable class in generated throw statements, we
 * basically throw the backing JavaScript error object so the generated code looks like this:
 * <pre>{@code
 *   try {
 *     throw $Exceptions.toJs(new SomeJavaException);
 *   } catch(e) {
 *     e = $Exceptions.toJava(e);
 *     if (SomeJavaException.$isInstance(e)) {
 *       // catch block for SomeJavaException
 *     } else {
 *       throw $Exception.toJs(e);
 *     }
 *   }
 * }</pre>
 *
 * <p> As the propagated thrown object is converted to real JavaScript error, it plays better with
 * the browser dev tools (doesn't work well with custom error objects) and callers from JavaScript
 * side.
 */
public class InsertExceptionConversionVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new InsertExceptionConversionVisitor());
  }

  @Override
  public Node rewriteCatchClause(CatchClause catchClause) {
    Variable mainVariable =
        new Variable(
            catchClause.getExceptionVar().getName(),
            TypeDescriptors.get().javaLangObject,
            false,
            false);

    MethodDescriptor toJava =
        MethodDescriptorBuilder.fromDefault()
            .isRaw(true)
            .isStatic(true)
            .enclosingClassTypeDescriptor(BootstrapType.EXCEPTIONS.getDescriptor())
            .methodName("wrap") // TODO(goktug): rename to toJava
            .parameterTypeDescriptors(TypeDescriptors.get().javaLangObject)
            .returnTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
            .build();

    MethodCall toJavaCall = MethodCall.createRegularMethodCall(null, toJava,
        mainVariable.getReference());

    Expression assignment =
        new BinaryExpression(
            mainVariable.getTypeDescriptor(),
            mainVariable.getReference(),
            BinaryOperator.ASSIGN,
            toJavaCall);

    catchClause.getBody().getStatements().add(0, new ExpressionStatement(assignment));

    return catchClause;
  }

  @Override
  public Node rewriteThrowStatement(ThrowStatement originalStatement) {
    MethodDescriptor toJs =
        MethodDescriptorBuilder.fromDefault()
            .isRaw(true)
            .isStatic(true)
            .enclosingClassTypeDescriptor(BootstrapType.EXCEPTIONS.getDescriptor())
            .methodName("unwrap") // TODO(goktug): rename to toJs
            .parameterTypeDescriptors(TypeDescriptors.get().javaLangThrowable)
            .returnTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .build();

    MethodCall toJsCall =
        MethodCall.createRegularMethodCall(null, toJs, originalStatement.getExpression());

    return new ThrowStatement(toJsCall);
  }
}
