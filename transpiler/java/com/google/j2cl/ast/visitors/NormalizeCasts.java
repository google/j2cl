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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

/** Replaces cast expression with corresponding cast method call. */
public class NormalizeCasts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteCastExpression(CastExpression expression) {
            TypeDescriptor castTypeDescriptor = expression.getCastTypeDescriptor();
            checkArgument(
                !castTypeDescriptor.isPrimitive(),
                "Narrowing and Widening conversions should have already converted"
                    + " all primitive casts.");
            if (castTypeDescriptor.isArray()) {
              return createArrayCastExpression(expression);
            }
            return createCastExpression(expression);
          }
        });
  }

  private static Expression createCastExpression(CastExpression castExpression) {
    checkArgument(!castExpression.getCastTypeDescriptor().isArray());
    checkArgument(!castExpression.getCastTypeDescriptor().isUnion());
    checkArgument(!castExpression.getCastTypeDescriptor().isIntersection());

    Expression resultingExpression = createCheckCastCall(castExpression);
    // /**@type {}*/ ()
    return JsDocCastExpression.newBuilder()
        .setExpression(resultingExpression)
        .setCastType(castExpression.getCastTypeDescriptor())
        .build();
  }

  private static Expression createCheckCastCall(CastExpression castExpression) {
    TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
    // Avoid pointlessly nesting type annotations inside of runtime cast calls.
    Expression expression = AstUtils.removeJsDocCastIfPresent(castExpression.getExpression());

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

  private static Expression createArrayCastExpression(CastExpression castExpression) {
    ArrayTypeDescriptor arrayCastTypeDescriptor =
        (ArrayTypeDescriptor) castExpression.getCastTypeDescriptor();

    // Arrays.$castTo(expr, leafType, dimension);
    MethodCall castMethodCall = createArrayCastCall(castExpression);
    // /**@type {}*/ ()
    return JsDocCastExpression.newBuilder()
        .setExpression(castMethodCall)
        .setCastType(arrayCastTypeDescriptor)
        .build();
  }

  private static MethodCall createArrayCastCall(CastExpression castExpression) {
    ArrayTypeDescriptor arrayCastTypeDescriptor =
        (ArrayTypeDescriptor) castExpression.getCastTypeDescriptor();
    TypeDescriptor leafTypeDescriptor = arrayCastTypeDescriptor.getLeafTypeDescriptor();

    if (leafTypeDescriptor.toRawTypeDescriptor().isNative()) {
      return RuntimeMethods.createArraysMethodCall("$castToNative", castExpression.getExpression());
    }

    return RuntimeMethods.createArraysMethodCall(
        "$castTo",
        castExpression.getExpression(),
        AstUtils.getMetadataConstructorReference(leafTypeDescriptor),
        NumberLiteral.fromInt(arrayCastTypeDescriptor.getDimensions()));
  }
}
