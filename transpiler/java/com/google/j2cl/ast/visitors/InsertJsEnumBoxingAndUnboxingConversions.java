/*
 * Copyright 2018 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.visitors.ConversionContextVisitor.ContextRewriter;

/** Inserts a boxing/unboxing operations required to support JsEnum semantics. */
public class InsertJsEnumBoxingAndUnboxingConversions extends NormalizationPass {
  // TODO(b/117510099): Revisit this pass to make it consistent with the way J2CL handles primitive
  // boxing/unboxing.
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteJsEnumBoxingConversionContext(Expression expression) {
                return box(expression);
              }

              @Override
              public Expression rewriteAssignmentContext(
                  TypeDescriptor toTypeDescriptor, Expression expression) {

                TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();
                if (AstUtils.isNonNativeJsEnum(fromTypeDescriptor)
                    && !AstUtils.isNonNativeJsEnum(toTypeDescriptor)) {
                  return box(expression);
                }

                if (AstUtils.isNonNativeJsEnum(toTypeDescriptor)
                    && !AstUtils.isNonNativeJsEnum(fromTypeDescriptor)) {
                  return unbox(toTypeDescriptor, expression);
                }
                return expression;
              }

              @Override
              public Expression rewriteMethodInvocationContext(
                  ParameterDescriptor parameterDescriptor, Expression argumentExpression) {
                if (parameterDescriptor.isDoNotAutobox()) {
                  return argumentExpression;
                }
                return rewriteAssignmentContext(
                    parameterDescriptor.getTypeDescriptor(), argumentExpression);
              }

              @Override
              public Expression rewriteCastContext(CastExpression castExpression) {
                TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
                Expression innerExpression = castExpression.getExpression();
                TypeDescriptor fromTypeDescriptor = innerExpression.getTypeDescriptor();

                // TODO(b/33681746): J2CL should not be emitting casts in these scenarios but it is.
                // remove when the bug is fixed.
                if (toTypeDescriptor.isJsEnum() && fromTypeDescriptor.isJsEnum()) {
                  checkState(toTypeDescriptor.hasSameRawType(fromTypeDescriptor));
                  // Remove the cast as it does not make sense casting on an unboxed value.
                  return innerExpression;
                }

                if (AstUtils.isNonNativeJsEnum(toTypeDescriptor)) {
                  // (MyJsEnum) o => unbox((MyJsEnum) o);
                  return unbox(toTypeDescriptor, castExpression);
                }

                if (AstUtils.isNonNativeJsEnum(fromTypeDescriptor)) {
                  // TODO(b/117431082): When information about super interfaces is represented
                  // accurately in the JsEnum type descriptor these casts are statically decidable.
                  // (Comparable) myJsEnum => (Comparable) box(myJsEnum);
                  return CastExpression.Builder.from(castExpression)
                      .setExpression(box(innerExpression))
                      .build();
                }

                return castExpression;
              }
            }));
  }

  private static Expression box(Expression expression) {
    return RuntimeMethods.createEnumsBoxMethodCall(expression);
  }

  private static Expression unbox(TypeDescriptor toTypeDescriptor, Expression expression) {
    return JsDocCastExpression.newBuilder()
        .setExpression(RuntimeMethods.createEnumsUnboxMethodCall(expression))
        .setCastType(toTypeDescriptor)
        .build();
  }
}
