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
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.MultiExpression;

/**
 * Rewrites field or method accesses of the form "instance.staticField" or "instance.staticMethod()"
 * to a multiexpression "(instance, SomeClass.staticField)" or (instance, SomeClass.staticMethod())"
 * since J2CL keeps the invariant that static member accesses are never qualified by an expression.
 */
public class NormalizeStaticMemberQualifiers extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMemberReference(MemberReference memberReference) {
            // If this is a static member referece, split the evaluation of the qualifier to
            // preserve
            // potential side effects.
            if (isStaticMemberReferenceWithInstanceQualifier(memberReference)) {
              MultiExpression.Builder multiExpressionBuilder = new MultiExpression.Builder();
              if (memberReference.getQualifier().hasSideEffects()) {
                multiExpressionBuilder.addExpressions(memberReference.getQualifier());
              }
              multiExpressionBuilder.addExpressions(
                  MemberReference.Builder.from(memberReference).setQualifier(null).build());
              return multiExpressionBuilder.build();
            }
            return memberReference;
          }
        });
  }

  /*** Returns true if a member reference to a static member is qualified by an expression.  */
  private static boolean isStaticMemberReferenceWithInstanceQualifier(Expression expression) {
    if (!(expression instanceof MemberReference)) {
      return false;
    }
    MemberReference memberReference = (MemberReference) expression;
    return memberReference.getTarget().isStatic() && memberReference.getQualifier() != null;
  }
}
