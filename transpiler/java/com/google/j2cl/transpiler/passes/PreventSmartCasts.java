/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.HashSet;
import java.util.Set;

/**
 * A pass which inserts trivial casts to avoid the kotlin compiler type errors due to smart casts.
 *
 * <p>Note: Smart cast are problematic when caused by casts that do not guarantee the exact type,
 * e.g. when casting to a parameterized type, the cast does not check that the parameterization
 * holds at runtime, but kotlinc would use the information about that parameterization to add smart
 * casts to, for example, field accesses. In the JVM the type checks related to the parameterization
 * are delayed and performed on accesses.
 */
public class PreventSmartCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            preventSmartCasts(method);
          }
        });
  }

  /**
   * Adds trivial casts to expressions in order to prevent smart casts.
   *
   * <p>Note: the pass relies on the fact that the postorder traversal traverses expressions in
   * evaluation order; after seeing a cast on a suitable expression, subsequent accesses will need a
   * spurious cast to prevent the smart cast in Kotlin.
   *
   * <p>This pass considers a subset of expressions where smart casts are observed in Kotlin. There
   * is no spec of where kotlinc decides to apply a smart cast.
   */
  private static void preventSmartCasts(Method method) {
    Set<Object> targetsTriggeringSmartCasts = new HashSet<>();
    method.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteVariableReference(VariableReference variableReference) {
            return processExpression(variableReference, variableReference.getTarget());
          }

          @Override
          public boolean shouldProcessFieldAccess(FieldAccess fieldAccess) {
            Member member = getCurrentMember();
            if (!member.isConstructor() && !member.isInitializerBlock()) {
              return true;
            }
            Expression qualifier = fieldAccess.getQualifier();
            if (qualifier instanceof ThisReference
                && fieldAccess
                    .getTarget()
                    .isMemberOf(member.getDescriptor().getEnclosingTypeDescriptor())
                && isModifiedByParent(fieldAccess, getParent())) {
              // Skip initialization of class fields, since these cannot have casts in the
              // qualifier and it would be an error to add one there.
              return false;
            }
            return true;
          }

          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            // As an approximation we do not consider the qualifier in a cast on a field access,
            // but this could easily be made more precise by consider a subset of qualifiers.
            return processExpression(fieldAccess, fieldAccess.getTarget());
          }

          @Override
          public Expression rewriteThisReference(ThisReference thisReference) {
            return processExpression(thisReference, thisReference.getTypeDescriptor());
          }

          /**
           * Records expressions that are cast and adds trivial cast to prevent smart cast if
           * needed.
           */
          private Expression processExpression(Expression expression, Object target) {
            if (getParent() instanceof CastExpression) {
              // There is an explicit cast on this expression, so it might trigger smart casts
              // down the line. Record it and return.
              targetsTriggeringSmartCasts.add(target);
              return expression;
            }

            if (targetsTriggeringSmartCasts.contains(target)
                && needsToPreventSmartCasts(expression)) {
              // Add cast to prevent smart cast.
              return CastExpression.newBuilder()
                  .setCastTypeDescriptor(expression.getTypeDescriptor())
                  .setExpression(expression)
                  .build();
            }

            return expression;
          }

          private boolean needsToPreventSmartCasts(Expression expression) {
            TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
            if (isModifiedByParent(expression, getParent())) {
              // Can't add casts to LHS of assignments, etc.
              return false;
            }

            if (typeDescriptor.isWildcardOrCapture() || !typeDescriptor.isDenotable()) {
              // Can't add casts to wildcards or non-denotable types.
              return false;
            }

            if (typeDescriptor instanceof DeclaredTypeDescriptor) {
              var declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
              if (!declaredTypeDescriptor.getTypeArgumentDescriptors().isEmpty()) {
                // Smart casts are problematic in parameterized types.
                return true;
              }
            }

            if (typeDescriptor instanceof ArrayTypeDescriptor) {
              // Arrays are modeled as parameterized types in kotlin; so smart casts are also
              // problematic here.
              return true;
            }

            return false;
          }
        });
  }

  private static boolean isModifiedByParent(Expression operand, Object parent) {
    if (parent instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) parent;
      return binaryExpression.isSimpleOrCompoundAssignment()
          && binaryExpression.getLeftOperand() == operand;
    }

    if (parent instanceof UnaryExpression) {
      UnaryExpression unaryExpression = (UnaryExpression) parent;
      return unaryExpression.isSimpleOrCompoundAssignment()
          && unaryExpression.getOperand() == operand;
    }

    return false;
  }
}
