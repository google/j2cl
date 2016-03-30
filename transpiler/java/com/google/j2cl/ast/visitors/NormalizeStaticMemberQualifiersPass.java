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
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeReference;

/**
 * Rewrites strange field or method accesses of the form "instance.staticField" to the more normal
 * and legal-in-JS form "SomeClass.staticField".
 *
 * <p>Sometimes the instance qualifier is more complicated and may contain a side effect that needs
 * to be preserved. So we'll sometimes rewrite "getInstance().staticField" to "(getInstance(),
 * SomeClass.staticField)".
 */
public class NormalizeStaticMemberQualifiersPass {

  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeStaticMemberQualifiersPass().rewriteSystemGetProperty(compilationUnit);
  }

  private void rewriteSystemGetProperty(CompilationUnit compilationUnit) {
    compilationUnit.accept(new FixQualifiers());
  }
  
  /**
   * Returns whether the member reference is statically accessed on a instance for example:
   * <p>new Instance().staticField;
   * <p>or
   * <p>new Instance().staticMethod();
   */
  private boolean isStaticMemberReferenceWithInstanceQualifier(Expression expression) {
    if (!(expression instanceof MemberReference)) {
      return false;
    }
    MemberReference memeberReference = (MemberReference) expression;
    return memeberReference.getTarget().isStatic()
        && !(memeberReference.getQualifier() instanceof TypeReference);
  }

  private class FixQualifiers extends AbstractRewriter {
    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      // If the access is of the very strange form "instance.staticField" then remove the qualifier
      // so that it is logically a "SomeClass.staticField".
      if (isStaticMemberReferenceWithInstanceQualifier(fieldAccess)) {
        return new MultiExpression(
            fieldAccess.getQualifier(), // Preserve side effects.
            new FieldAccess(null, fieldAccess.getTarget()) // Static dispatch
            );
      }

      return fieldAccess;
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      // If the access is of the very strange form "instance.staticMethod()" then remove the
      // qualifier so that it is logically a "SomeClass.staticMethod()".
      if (isStaticMemberReferenceWithInstanceQualifier(methodCall)) {
        return new MultiExpression(
            methodCall.getQualifier(), // Preserve side effects.
            MethodCall.Builder.from(methodCall).qualifier(null).build() // Static dispatch
            );
      }
      return methodCall;
    }
  }
}
