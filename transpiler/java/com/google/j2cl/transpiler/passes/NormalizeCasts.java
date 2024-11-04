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
import static com.google.j2cl.transpiler.ast.AstUtils.isBoxableJsEnumType;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
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
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.transpiler.ast.TypeVariable;

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
              return createClosureTypeCast(typeDescriptor, expression);
            }
            return castExpression;
          }
        });
  }

  private static boolean canRemoveCast(TypeDescriptor castTypeDescriptor, Expression expression) {
    return castTypeDescriptor.isNoopCast() || isRedundantCast(castTypeDescriptor, expression);
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
    if (isAssignableTo(typeDescriptor, expression)) {
      return true;
    }
    expression = skipPassThroughExpressions(expression);

    if (expression instanceof CastExpression) {
      CastExpression castExpression = (CastExpression) expression;
      return castExpression.getTypeDescriptor().isAssignableTo(typeDescriptor)
          || isRedundantCast(typeDescriptor, castExpression.getExpression());
    }
    return isAssignableTo(typeDescriptor, expression);
  }

  private static boolean isAssignableTo(TypeDescriptor castTypeDescriptor, Expression expression) {
    return expression instanceof NullLiteral
        || expression
            .getDeclaredTypeDescriptor()
            .toRawTypeDescriptor()
            .isAssignableTo(castTypeDescriptor);
  }

  private static Expression skipPassThroughExpressions(Expression expression) {
    if (expression instanceof MultiExpression) {
      return skipPassThroughExpressions(
          Iterables.getLast(((MultiExpression) expression).getExpressions()));
    }
    if (expression instanceof JsDocCastExpression) {
      return skipPassThroughExpressions(((JsDocCastExpression) expression).getExpression());
    }
    if (expression instanceof PostfixExpression
        && ((PostfixExpression) expression).getOperator() == PostfixOperator.NOT_NULL_ASSERTION) {
      return skipPassThroughExpressions(((PostfixExpression) expression).getOperand());
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

            // Casts have two purposes:
            //   1. perform the runtime check required by Java semantics.
            //   2. communicate the resulting type to closure.

            // /** @type {X} */ (Casts.to(expression, JavaType))
            return createClosureTypeCast(
                castTypeDescriptor, implementRuntimeCheck(castTypeDescriptor, expression));
          }
        });
  }

  private static Expression createClosureTypeCast(
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
    if (isBoxableJsEnumType(castTypeDescriptor)) {
      // TODO(b/118615488): Surface enum boxed types so that this hack is not needed.
      castTypeDescriptor = TypeDescriptors.getEnumBoxType(castTypeDescriptor);
    }
    return JsDocCastExpression.newBuilder()
        .setCastTypeDescriptor(castTypeDescriptor)
        .setExpression(expression)
        .build();
  }

  private static Expression implementRuntimeCheck(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    checkArgument(
        !toTypeDescriptor.isPrimitive(),
        "Narrowing and Widening conversions should have removed all primitive casts.");

    if (toTypeDescriptor.isTypeVariable()) {
      TypeDescriptor bound = ((TypeVariable) toTypeDescriptor).getUpperBoundTypeDescriptor();
      // Do a runtime check on the type of the bound, and not that there can not be an infinite
      // recursion here since the recursive use of a type variable always occurs as a type
      // argument of a declared type.
      return implementRuntimeCheck(bound, expression);
    }

    if (toTypeDescriptor.isIntersection()) {
      return implementRuntimeCheckForIntersection(
          (IntersectionTypeDescriptor) toTypeDescriptor, expression);
    }

    if (toTypeDescriptor.isArray()) {
      return createRuntimeCheckForArray((ArrayTypeDescriptor) toTypeDescriptor, expression);
    }

    return createRuntimeCheckForDeclaredType((DeclaredTypeDescriptor) toTypeDescriptor, expression);
  }

  private static Expression implementRuntimeCheckForIntersection(
      IntersectionTypeDescriptor intersectionTypeDescriptor, Expression expression) {
    // Emit the casts so that the first type in the intersection corresponds to the
    // innermost cast.
    for (TypeDescriptor intersectedTypeDescriptor :
        intersectionTypeDescriptor.getIntersectionTypeDescriptors()) {
      if (!canRemoveCast(intersectedTypeDescriptor, expression)) {
        expression = implementRuntimeCheck(intersectedTypeDescriptor, expression);
      }
    }
    return expression;
  }

  private static Expression createRuntimeCheckForArray(
      ArrayTypeDescriptor arrayCastTypeDescriptor, Expression expression) {
    // Avoid pointlessly nesting type annotations inside of runtime cast calls.
    expression = AstUtils.removeJsDocCastIfPresent(expression);

    // Arrays.$castTo(expr, leafType, dimension);
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

  private static Expression createRuntimeCheckForDeclaredType(
      DeclaredTypeDescriptor toTypeDescriptor, Expression expression) {

    // Avoid pointlessly nesting type annotations inside of runtime cast calls.
    expression = AstUtils.removeJsDocCastIfPresent(expression);

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setOriginalJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(BootstrapType.CASTS.getDescriptor())
            .setName("$to")
            .setParameterTypeDescriptors(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangObject)
            .setReturnTypeDescriptor(toTypeDescriptor)
            .build();

    // Casts.$to(expr, TypeName);
    return MethodCall.Builder.from(castToMethodDescriptor)
        .setArguments(expression, toTypeDescriptor.getMetadataConstructorReference())
        .build();
  }
}
