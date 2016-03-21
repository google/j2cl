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
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces cast expression with corresponding cast method call.
 */
public class NormalizeCastsVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeCastsVisitor().normalizeCasts(compilationUnit);
  }

  private void normalizeCasts(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteCastExpression(CastExpression expression) {
    if (expression.isRaw()) {
      return expression;
    }
    TypeDescriptor castTypeDescriptor = expression.getCastTypeDescriptor();
    Preconditions.checkArgument(
        !castTypeDescriptor.isPrimitive(),
        "Narrowing and Widening conversions should have already converted all primitive casts.");

    if (castTypeDescriptor.isArray()) {
      return rewriteArrayCastExpression(expression);
    }
    return rewriteRegularCastExpression(expression);
  }

  private Node rewriteRegularCastExpression(CastExpression castExpression) {
    Preconditions.checkArgument(
        castExpression.getCastTypeDescriptor() instanceof RegularTypeDescriptor);
    TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
    TypeDescriptor rawCastTypeDescriptor =
        castExpression.getCastTypeDescriptor().getRawTypeDescriptor();
    Expression expression = castExpression.getExpression();

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .jsInfo(JsInfo.RAW)
            .isStatic(true)
            .enclosingClassTypeDescriptor(BootstrapType.CASTS.getDescriptor())
            .methodName("to")
            .parameterTypeDescriptors(
                Lists.newArrayList(
                    TypeDescriptors.get().javaLangObject, TypeDescriptors.get().primitiveBoolean))
            .returnTypeDescriptor(castTypeDescriptor)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(expression);
    TypeDescriptor castTypeDescriptorArgument =
        rawCastTypeDescriptor.isNative()
            ? AstUtils.createJsOverlayImplTypeDescriptor(rawCastTypeDescriptor)
            : rawCastTypeDescriptor;
    Preconditions.checkArgument(
        !castTypeDescriptorArgument.isNative(),
        "Should not pass a native type to Arrays.$castTo().");
    arguments.add(new TypeReference(castTypeDescriptorArgument));

    // Casts.to(expr, TypeName);
    MethodCall castMethodCall =
        MethodCall.createRegularMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return CastExpression.Builder.from(castExpression)
        .isRaw(true)
        .expression(castMethodCall)
        .build();
  }

  private Node rewriteArrayCastExpression(CastExpression castExpression) {
    Preconditions.checkArgument(
        castExpression.getCastTypeDescriptor() instanceof ArrayTypeDescriptor);
    TypeDescriptor arrayCastTypeDescriptor = castExpression.getCastTypeDescriptor();
    if (arrayCastTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor().isNative()) {
      return rewriteNativeJsArrayCastExpression(castExpression);
    }
    return rewriteJavaArrayCastExpression(castExpression);
  }

  private Node rewriteJavaArrayCastExpression(CastExpression castExpression) {
    TypeDescriptor arrayCastTypeDescriptor = castExpression.getCastTypeDescriptor();
    MethodDescriptor castToMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .jsInfo(JsInfo.RAW)
            .isStatic(true)
            .enclosingClassTypeDescriptor(BootstrapType.ARRAYS.getDescriptor())
            .methodName("$castTo")
            .parameterTypeDescriptors(
                Lists.newArrayList(
                    TypeDescriptors.get().javaLangObject,
                    TypeDescriptors.get().javaLangObject,
                    TypeDescriptors.get().primitiveInt))
            .returnTypeDescriptor(arrayCastTypeDescriptor)
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
        MethodCall.createRegularMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return CastExpression.createRaw(castMethodCall, arrayCastTypeDescriptor);
  }

  private Node rewriteNativeJsArrayCastExpression(CastExpression castExpression) {
    TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
    Preconditions.checkArgument(castTypeDescriptor.isArray());
    Preconditions.checkArgument(
        castTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor().isNative());
    MethodDescriptor castToMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .jsInfo(JsInfo.RAW)
            .isStatic(true)
            .enclosingClassTypeDescriptor(BootstrapType.ARRAYS.getDescriptor())
            .methodName("$castToNative")
            .parameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.get().javaLangObject))
            .returnTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(castExpression.getExpression());

    // Arrays.$castToNative(expr);
    MethodCall castMethodCall =
        MethodCall.createRegularMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return CastExpression.createRaw(castMethodCall, castTypeDescriptor);
  }
}
