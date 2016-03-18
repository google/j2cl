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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodCallBuilder;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;

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
    compilationUnit.accept(new FixFieldAssignmentQualifiers());
    compilationUnit.accept(new FixOtherQualifiers());
  }

  private class FixFieldAssignmentQualifiers extends AbstractRewriter {
    @Override
    public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
      // Only look at assignments into fields where the field has an instance qualifier.
      if (!binaryExpression.getOperator().doesAssignment()) {
        return binaryExpression;
      }
      if (!(binaryExpression.getLeftOperand() instanceof FieldAccess)) {
        return binaryExpression;
      }
      FieldAccess fieldAccess = (FieldAccess) binaryExpression.getLeftOperand();
      if (!fieldAccess.getTarget().isStatic()
          || fieldAccess.getQualifier() instanceof TypeDescriptor) {
        return binaryExpression;
      }

      // For the node on the left hand side of an assignment you only need to handle field accesses
      // and not method calls because method calls are illegal there (you can't assign into a method
      // call).
      return new MultiExpression(
          Lists.newArrayList(
              fieldAccess.getQualifier(), // Preserve the side effect.
              new BinaryExpression( // Rewrite the assignment without the qualifier.
                  binaryExpression.getTypeDescriptor(),
                  new FieldAccess(null, fieldAccess.getTarget()),
                  binaryExpression.getOperator(),
                  binaryExpression.getRightOperand())));
    }
  }

  private class FixOtherQualifiers extends AbstractRewriter {
    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      // If the access is of the very strange form "instance.staticField" then remove the qualifier
      // so that it is logically a "SomeClass.staticField".
      if (fieldAccess.getTarget().isStatic()
          && !(fieldAccess.getQualifier() instanceof TypeDescriptor)) {
        return new MultiExpression(
            Lists.newArrayList(
                fieldAccess.getQualifier(), // Preserve side effects.
                new FieldAccess(null, fieldAccess.getTarget())) // Static dispatch
        );
      }

      return fieldAccess;
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      // If the access is of the very strange form "instance.staticMethod()" then remove the
      // qualifier so that it is logically a "SomeClass.staticMethod()".
      if (methodCall.getTarget().isStatic()
          && !(methodCall.getQualifier() instanceof TypeDescriptor)) {
        return new MultiExpression(
            Lists.newArrayList(
                methodCall.getQualifier(), // Preserve side effects.
                MethodCallBuilder.from(methodCall).qualifier(null).build()) // Static dispatch
        );
      }
      return methodCall;
    }
  }
}
