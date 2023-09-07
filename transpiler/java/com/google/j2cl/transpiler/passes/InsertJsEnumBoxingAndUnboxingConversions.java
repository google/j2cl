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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/** Inserts a boxing/unboxing operations required to support JsEnum semantics. */
public class InsertJsEnumBoxingAndUnboxingConversions extends NormalizationPass {
  // TODO(b/117510099): Revisit this pass to make it consistent with the way J2CL handles primitive
  // boxing/unboxing.
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // TODO(b/118615488): Surface the BoxedLightEnum type to the compiler and make this pass
    // simpler.
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteJsEnumBoxingConversionContext(Expression expression) {
                return box(expression);
              }

              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor toDeclaredTypeDescriptor,
                  Expression expression) {
                TypeDescriptor fromTypeDescriptor = expression.getDeclaredTypeDescriptor();
                if (AstUtils.isNonNativeJsEnum(fromTypeDescriptor)
                    && !AstUtils.isNonNativeJsEnum(toDeclaredTypeDescriptor)) {
                  return box(expression);
                }

                if (AstUtils.isNonNativeJsEnum(toDeclaredTypeDescriptor)
                    && !AstUtils.isNonNativeJsEnum(fromTypeDescriptor)) {
                  return unbox(toDeclaredTypeDescriptor, expression);
                }
                return expression;
              }

              @Override
              public Expression rewriteMethodInvocationContext(
                  ParameterDescriptor inferredParameterDescriptor,
                  ParameterDescriptor declaredParameterDescriptor,
                  Expression argument) {
                if (inferredParameterDescriptor.isDoNotAutobox()) {
                  return argument;
                }
                return rewriteTypeConversionContext(
                    inferredParameterDescriptor.getTypeDescriptor(),
                    declaredParameterDescriptor.getTypeDescriptor(),
                    argument);
              }

              @Override
              public Expression rewriteCastContext(CastExpression castExpression) {
                TypeDescriptor toTypeDescriptor = castExpression.getCastTypeDescriptor();
                // Make sure when parameters are specialized to JsEnum they are unboxed.
                Expression innerExpression = castExpression.getExpression();
                TypeDescriptor fromTypeDescriptor = innerExpression.getDeclaredTypeDescriptor();

                if (toTypeDescriptor.isJsEnum() && fromTypeDescriptor.isJsEnum()) {
                  checkState(toTypeDescriptor.isSameBaseType(fromTypeDescriptor));
                  // This is a redundant cast, remove it.
                  return innerExpression;
                }

                if (AstUtils.isNonNativeJsEnum(toTypeDescriptor)) {
                  // (MyJsEnum) o => unbox(o, <MyJsEnum.class>);
                  return unbox(toTypeDescriptor, innerExpression);
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
        .setExpression(RuntimeMethods.createEnumsUnboxMethodCall(expression, toTypeDescriptor))
        .setCastType(toTypeDescriptor)
        .build();
  }
}
