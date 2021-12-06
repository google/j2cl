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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;

/** Replaces cast expression with corresponding cast method call. */
public class NormalizeCasts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    removeRedundantCasts(compilationUnit);
    implementCasts(compilationUnit);
  }

  /**
   * Removes redundant cast checks that might have been introduced during normalization passes.
   *
   * <p>Redundant casts are replaced by JsDocCasts to preserve the type of the resulting expression.
   */
  private void removeRedundantCasts(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(CastExpression castExpression) {
            TypeDescriptor typeDescriptor = castExpression.getTypeDescriptor();

            Expression expression = castExpression.getExpression();
            if (canRemoveCast(typeDescriptor, expression)) {
              // No need to perform a cast check but to communicate the right type to jscompiler a
              // JsDoc cast is emitted instead.
              return createJsDocCast(typeDescriptor, expression);
            }
            return castExpression;
          }
        });
  }

  private static boolean canRemoveCast(TypeDescriptor castTypeDescriptor, Expression expression) {
    boolean isStaticallyGuaranteedToHoldAtRuntime =
        expression instanceof NullLiteral
            || expression
                .getDeclaredTypeDescriptor()
                .toRawTypeDescriptor()
                .isAssignableTo(castTypeDescriptor);
    return castTypeDescriptor.isNoopCast()
        || isStaticallyGuaranteedToHoldAtRuntime
        || isRedundantCast(castTypeDescriptor, expression);
  }

  /**
   * Returns true if the inner expression already cast to this type.
   *
   * <p>Due to normalization the code might end up with sequences of casts that might be redundant,
   * code like e.g.:
   *
   * <p>
   *
   * <pre><code>
   *    ((A &amp; B &amp; C) expr).methodOfC();
   *  </code></pre>
   *
   * <p>will be normalized to:
   *
   * <p>
   *
   * <pre><code>
   *    ((C) (A &amp; B &amp; C) expr).methodOfC();
   *  </code></pre>
   */
  private static boolean isRedundantCast(TypeDescriptor typeDescriptor, Expression expression) {
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

  /**
   * Replaces casts with the appropriate calls to the runtime to perform the corresponding type
   * checks.
   */
  private void implementCasts(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(CastExpression castExpression) {
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

  private static Expression createCastExpression(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    checkArgument(
        !toTypeDescriptor.isArray()
            && !toTypeDescriptor.isUnion()
            && !toTypeDescriptor.isIntersection());

    return createJsDocCast(toTypeDescriptor, createCastsToCall(toTypeDescriptor, expression));
  }

  private static Expression createCastsToCall(
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
        .setArguments(expression, castTypeDescriptor.getMetadataConstructorReference())
        .build();
  }

  private static Expression createIntersectionCastExpression(
      IntersectionTypeDescriptor intersectionTypeDescriptor, Expression expression) {
    // Emit the casts so that the first type in the intersection corresponds to the
    // innermost cast.
    for (TypeDescriptor intersectedTypeDescriptor :
        intersectionTypeDescriptor.getIntersectionTypeDescriptors()) {
      if (!canRemoveCast(intersectedTypeDescriptor, expression)) {
        expression = createCastExpression(intersectedTypeDescriptor, expression);
      }
    }
    // /**@type {}*/ (...)
    return createJsDocCast(intersectionTypeDescriptor, expression);
  }

  private static Expression createArrayCastExpression(
      ArrayTypeDescriptor arrayCastTypeDescriptor, Expression expression) {
    // Avoid pointlessly nesting type annotations inside of runtime cast calls.
    expression = AstUtils.removeJsDocCastIfPresent(expression);

    // Arrays.$castTo(expr, leafType, dimension);
    MethodCall castMethodCall = createArraysCastToCall(arrayCastTypeDescriptor, expression);
    // /**@type {}*/ (...)
    return createJsDocCast(arrayCastTypeDescriptor, castMethodCall);
  }

  private static MethodCall createArraysCastToCall(
      ArrayTypeDescriptor arrayCastTypeDescriptor, Expression expression) {
    TypeDescriptor leafTypeDescriptor = arrayCastTypeDescriptor.getLeafTypeDescriptor();

    if (leafTypeDescriptor.toRawTypeDescriptor().isNative()) {
      return RuntimeMethods.createArraysMethodCall("$castToNative", expression);
    }

    return RuntimeMethods.createArraysMethodCall(
        "$castTo",
        expression,
        leafTypeDescriptor.getMetadataConstructorReference(),
        NumberLiteral.fromInt(arrayCastTypeDescriptor.getDimensions()));
  }

  private static Expression createJsDocCast(
      TypeDescriptor castTypeDescriptor, Expression expression) {
    if (castTypeDescriptor.isIntersection()) {
      // Annotate the expression so that is typed (in closure) with the first type in the
      // intersection. Intersection types do not have a direct representation in closure.
      // Since in general we need a consistent view of the closure type of expressions,
      // we chose the to see expressions typed at an intersection cast as being explicitly
      // typed at the first component, which is also consistent with the JVM type (the erasure
      // of an intersection cast is the erasure of its first component).
      castTypeDescriptor = ((IntersectionTypeDescriptor) castTypeDescriptor).getFirstType();
    }
    if (AstUtils.isNonNativeJsEnum(castTypeDescriptor)) {
      // TODO(b/118615488): Surface enum boxed types so that this hack is not needed.
      castTypeDescriptor = TypeDescriptors.getEnumBoxType(castTypeDescriptor);
    }
    return JsDocCastExpression.newBuilder()
        .setCastType(castTypeDescriptor)
        .setExpression(expression)
        .build();
  }
}
