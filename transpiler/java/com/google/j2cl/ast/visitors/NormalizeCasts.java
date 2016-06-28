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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsTypeAnnotation;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeReference;

import java.util.ArrayList;
import java.util.List;

/** Replaces cast expression with corresponding cast method call. */
public class NormalizeCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteCastExpression(CastExpression expression) {
      TypeDescriptor castTypeDescriptor = expression.getCastTypeDescriptor();
      Preconditions.checkArgument(
          !castTypeDescriptor.isPrimitive(),
          "Narrowing and Widening conversions should have already converted all primitive casts.");
      if (castTypeDescriptor.isArray()) {
        return createArrayCastExpression(expression);
      }
      return createCastExpression(expression);
    }
  }

  private static Node createCastExpression(CastExpression castExpression) {
    Preconditions.checkArgument(!castExpression.getCastTypeDescriptor().isArray());
    Preconditions.checkArgument(!castExpression.getCastTypeDescriptor().isUnion());

    TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
    TypeDescriptor rawCastTypeDescriptor =
        castExpression.getCastTypeDescriptor().getRawTypeDescriptor();
    Expression expression = castExpression.getExpression();

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.CASTS.getDescriptor())
            .setMethodName("to")
            .setParameterTypeDescriptors(
                Lists.newArrayList(
                    TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangObject))
            .setReturnTypeDescriptor(castTypeDescriptor)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(expression);
    TypeDescriptor castTypeDescriptorArgument =
        rawCastTypeDescriptor.isNative()
            ? AstUtils.createOverlayImplementationClassTypeDescriptor(rawCastTypeDescriptor)
            : rawCastTypeDescriptor;
    Preconditions.checkArgument(
        !castTypeDescriptorArgument.isNative(),
        "Should not pass a native type to Arrays.$castTo().");
    arguments.add(new TypeReference(castTypeDescriptorArgument));

    // Casts.to(expr, TypeName);
    MethodCall castMethodCall =
        MethodCall.createMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return JsTypeAnnotation.createTypeAnnotation(
        castMethodCall, castExpression.getCastTypeDescriptor());
  }

  private static Node createArrayCastExpression(CastExpression castExpression) {
    Preconditions.checkArgument(castExpression.getCastTypeDescriptor().isArray());

    if (castExpression
        .getCastTypeDescriptor()
        .getLeafTypeDescriptor()
        .getRawTypeDescriptor()
        .isNative()) {
      return createNativeJsArrayCastExpression(castExpression);
    }
    return createJavaArrayCastExpression(castExpression);
  }

  private static Node createJavaArrayCastExpression(CastExpression castExpression) {
    TypeDescriptor arrayCastTypeDescriptor = castExpression.getCastTypeDescriptor();
    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.ARRAYS.getDescriptor())
            .setMethodName("$castTo")
            .setParameterTypeDescriptors(
                Lists.newArrayList(
                    TypeDescriptors.get().javaLangObject,
                    TypeDescriptors.get().javaLangObject,
                    TypeDescriptors.get().primitiveInt))
            .setReturnTypeDescriptor(arrayCastTypeDescriptor)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(castExpression.getExpression());
    TypeDescriptor castTypeDescriptorArgument =
        arrayCastTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor();
    Preconditions.checkArgument(
        !castTypeDescriptorArgument.isNative(),
        "Should not pass a native type to Arrays.$castTo().");
    arguments.add(new TypeReference(castTypeDescriptorArgument));
    arguments.add(
        new NumberLiteral(
            TypeDescriptors.get().primitiveInt, arrayCastTypeDescriptor.getDimensions()));

    // Arrays.$castTo(expr, leafType, dimension);
    MethodCall castMethodCall =
        MethodCall.createMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return JsTypeAnnotation.createTypeAnnotation(castMethodCall, arrayCastTypeDescriptor);
  }

  private static Node createNativeJsArrayCastExpression(CastExpression castExpression) {
    TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
    Preconditions.checkArgument(
        castTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor().isNative());
    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.ARRAYS.getDescriptor())
            .setMethodName("$castToNative")
            .setParameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.get().javaLangObject))
            .setReturnTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(castExpression.getExpression());

    // Arrays.$castToNative(expr);
    MethodCall castMethodCall =
        MethodCall.createMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return JsTypeAnnotation.createTypeAnnotation(castMethodCall, castTypeDescriptor);
  }
}
