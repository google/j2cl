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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.IntersectionTypeDescriptor;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/** Replaces cast expression with corresponding cast method call. */
public class NormalizeCasts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Remove redundant cast checks that might have been introduced during normalization passes.
    // Redundant casts are replaced by JsDocCasts to preserve the type of the resulting expression.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(CastExpression castExpression) {
            TypeDescriptor typeDescriptor = castExpression.getTypeDescriptor();

            Expression expression = castExpression.getExpression();
            if (isRedundantCast(typeDescriptor, expression)) {
              // Replace cast with jsdoc cast since the type was already checked in the inner
              // expression.
              return JsDocCastExpression.newBuilder()
                  .setExpression(expression)
                  .setCastType(typeDescriptor)
                  .build();
            }
            return castExpression;
          }
        });
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteCastExpression(CastExpression castExpression) {
            TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
            Expression expression = castExpression.getExpression();

            checkArgument(
                !castTypeDescriptor.isPrimitive(),
                "Narrowing and Widening conversions should have removed all primitive casts.");

            if (castTypeDescriptor.isArray()) {
              return createArrayCastExpression(
                  (ArrayTypeDescriptor) castTypeDescriptor, expression);
            }

            if (castTypeDescriptor.isIntersection()) {
              return createIntersectionCastExpression(
                  (IntersectionTypeDescriptor) castTypeDescriptor, expression);
            }

            return createCastExpression(castTypeDescriptor, expression);
          }
        });
  }

  /** Returns true if the inner expression already cast to this type. */
  private boolean isRedundantCast(TypeDescriptor typeDescriptor, Expression expression) {
    expression = skipPassThroughExpressions(expression);

    if (expression instanceof CastExpression) {
      CastExpression castExpression = (CastExpression) expression;
      return castExpression.getTypeDescriptor().isAssignableTo(typeDescriptor)
          || isRedundantCast(typeDescriptor, castExpression.getExpression());
    }
    return false;
  }

  private static Expression skipPassThroughExpressions(Expression expression) {
    if (expression instanceof MultiExpression) {
      return skipPassThroughExpressions(
          Iterables.getLast(((MultiExpression) expression).getExpressions()));
    }
    if (expression instanceof JsDocCastExpression) {
      return skipPassThroughExpressions(((JsDocCastExpression) expression).getExpression());
    }
    return expression;
  }

  private static Expression createCastExpression(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    checkArgument(
        !toTypeDescriptor.isArray()
            && !toTypeDescriptor.isUnion()
            && !toTypeDescriptor.isIntersection());

    Expression resultingExpression = createCheckCastCall(toTypeDescriptor, expression);
    // /**@type {}*/ ()
    return AstUtils.isNonNativeJsEnum(toTypeDescriptor)
        ? resultingExpression
        : JsDocCastExpression.newBuilder()
            .setExpression(resultingExpression)
            .setCastType(toTypeDescriptor)
            .build();
  }

  private static Expression createCheckCastCall(
      TypeDescriptor castTypeDescriptor, Expression expression) {
    // Avoid pointlessly nesting type annotations inside of runtime cast calls.
    expression = AstUtils.removeJsDocCastIfPresent(expression);

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(BootstrapType.CASTS.getDescriptor())
            .setName("$to")
            .setParameterTypeDescriptors(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangObject)
            .setReturnTypeDescriptor(castTypeDescriptor)
            .build();

    // Casts.$to(expr, TypeName);
    return MethodCall.Builder.from(castToMethodDescriptor)
        .setArguments(expression, AstUtils.getMetadataConstructorReference(castTypeDescriptor))
        .build();
  }

  private static Expression createIntersectionCastExpression(
      IntersectionTypeDescriptor intersectionTypeDescriptor, Expression expression) {
    // Emit the casts so that the first type in the intersection corresponds to the
    // innermost cast.
    for (TypeDescriptor intersectedTypeDescriptor :
        intersectionTypeDescriptor.getIntersectionTypeDescriptors()) {
      expression = createCastExpression(intersectedTypeDescriptor, expression);
    }
    // Annotate the expression so that is typed (in closure) with the first type in the
    // intersection. Intersection types do not have a direct representation in closure.
    // Since in general we need a consistent view of the closure type of expressions,
    // we chose the to see expressions typed at an intersection cast as being explicitly
    // typed at the first component, which is also consistent with the JVM type (the erasure
    // of an intersection cast is the erasure of its first component).
    return JsDocCastExpression.newBuilder()
        .setCastType(intersectionTypeDescriptor.getFirstType())
        .setExpression(expression)
        .build();
  }

  private static Expression createArrayCastExpression(
      ArrayTypeDescriptor arrayCastTypeDescriptor, Expression expression) {

    // Arrays.$castTo(expr, leafType, dimension);
    MethodCall castMethodCall = createArrayCastCall(arrayCastTypeDescriptor, expression);
    // /**@type {}*/ ()
    return JsDocCastExpression.newBuilder()
        .setExpression(castMethodCall)
        .setCastType(arrayCastTypeDescriptor)
        .build();
  }

  private static MethodCall createArrayCastCall(
      ArrayTypeDescriptor arrayCastTypeDescriptor, Expression expression) {
    TypeDescriptor leafTypeDescriptor = arrayCastTypeDescriptor.getLeafTypeDescriptor();

    if (leafTypeDescriptor.toRawTypeDescriptor().isNative()) {
      return RuntimeMethods.createArraysMethodCall("$castToNative", expression);
    }

    return RuntimeMethods.createArraysMethodCall(
        "$castTo",
        expression,
        AstUtils.getMetadataConstructorReference(leafTypeDescriptor),
        NumberLiteral.fromInt(arrayCastTypeDescriptor.getDimensions()));
  }
}
