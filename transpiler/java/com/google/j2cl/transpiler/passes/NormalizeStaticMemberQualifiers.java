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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;

/**
 * Rewrites strange field or method accesses of the form "instance.staticField" to the more normal
 * and legal-in-JS form "SomeClass.staticField".
 *
 * <p>Sometimes the instance qualifier is more complicated and may contain a side effect that needs
 * to be preserved. So we'll sometimes rewrite "getInstance().staticField" to "(getInstance(),
 * SomeClass.staticField)".
 */
public class NormalizeStaticMemberQualifiers extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            // If the access is of the very strange form "instance.staticField" then remove
            // the qualifier so that it is logically a "SomeClass.staticField".
            if (isStaticMemberReferenceWithInstanceQualifier(fieldAccess)) {
              return MultiExpression.newBuilder()
                  .setExpressions(
                      fieldAccess.getQualifier(), // Preserve side effects.
                      FieldAccess.Builder.from(fieldAccess.getTarget())
                          .setQualifier(null) // Static dispatch.
                          .build())
                  .build();
            }
            return fieldAccess;
          }

          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            // If the access is of the very strange form "instance.staticMethod()" then remove the
            // qualifier so that it is logically a "SomeClass.staticMethod()".
            if (isStaticMemberReferenceWithInstanceQualifier(methodCall)) {
              return MultiExpression.newBuilder()
                  .setExpressions(
                      methodCall.getQualifier(), // Preserve side effects.
                      MethodCall.Builder.from(methodCall)
                          .setQualifier(null) // Static dispatch.
                          .build())
                  .build();
            }
            return methodCall;
          }
        });
  }

  /**
   * Returns whether the member reference is statically accessed on a instance for example:
   *
   * <p>new Instance().staticField;
   *
   * <p>or
   *
   * <p>new Instance().staticMethod();
   */
  private static boolean isStaticMemberReferenceWithInstanceQualifier(Expression expression) {
    if (!(expression instanceof MemberReference)) {
      return false;
    }
    MemberReference memberReference = (MemberReference) expression;
    return memberReference.getTarget().isStatic() && memberReference.getQualifier() != null;
  }
}
