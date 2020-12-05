/*
 * Copyright 2017 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;

/**
 * Normalizes @JsFunction property invocations so that they don't capture the qualifier
 * accidentally.
 *
 * <p>In the context of the following code:
 *
 * <pre>{@code
 * class A {
 *   JsFunctionInterface property;
 * }
 * }</pre>
 *
 * <p>When a function property is accessed and called as this Java code:
 *
 * <pre>
 *   {@code new A().property.m();}
 * </pre>
 *
 * <p>Before:
 *
 * <pre>
 * {@code new A().property();}
 * </pre>
 *
 * which will make the function capture the qualifier {@code new A()} as {@code this}.
 *
 * <p>After:
 *
 * <pre>
 *   {@code (var $function = new A().property, $function());}
 * </pre>
 *
 * which will prevent accidental capture of {@code this}.
 */
public class NormalizeJsFunctionPropertyInvocations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor targetMethod = methodCall.getTarget();
            Expression qualifier = methodCall.getQualifier();
            if (!targetMethod.getEnclosingTypeDescriptor().isJsFunctionInterface()
                || !doesQualifierCaptureInstance(qualifier)) {
              return methodCall;
            }

            Variable qualifierVariable =
                Variable.newBuilder()
                    .setFinal(true)
                    .setName("$function")
                    .setTypeDescriptor(qualifier.getTypeDescriptor())
                    .build();
            return MultiExpression.newBuilder()
                .setExpressions(
                    // Declare the temporary variable and initialize to the evaluated qualifier.
                    VariableDeclarationExpression.newBuilder()
                        .addVariableDeclaration(qualifierVariable, qualifier)
                        .build(),
                    MethodCall.Builder.from(methodCall)
                        .setQualifier(qualifierVariable.createReference())
                        .build())
                .build();
          }
        });
  }

  /**
   * Return true for qualifiers that will be emitted as field accesses and can accidentally be
   * captured by the function property.
   */
  private static boolean doesQualifierCaptureInstance(Expression qualifier) {
    return qualifier instanceof FieldAccess
        || qualifier instanceof ArrayAccess
        || qualifier instanceof MultiExpression
        || qualifier instanceof ConditionalExpression
        || qualifier instanceof JsDocCastExpression
        || (qualifier instanceof MethodCall
            && ((MethodCall) qualifier).getTarget().isJsPropertyGetter());
  }
}
