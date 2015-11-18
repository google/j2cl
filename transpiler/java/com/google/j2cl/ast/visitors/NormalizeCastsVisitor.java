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
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Visibility;

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
    RegularTypeDescriptor castTypeDescriptor =
        (RegularTypeDescriptor) castExpression.getCastTypeDescriptor();
    Expression expression = castExpression.getExpression();

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.createRaw(
            true,
            Visibility.PUBLIC,
            BootstrapType.CASTS.getDescriptor(),
            "to",
            Lists.newArrayList(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().primitiveBoolean),
            castTypeDescriptor,
            null,
            null,
            false);
    List<Expression> arguments = new ArrayList<>();
    arguments.add(expression);
    arguments.add(castTypeDescriptor.getRawTypeDescriptor());

    // Casts.to(expr, TypeName);
    MethodCall castMethodCall =
        MethodCall.createRegularMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return CastExpression.createRaw(castMethodCall, castTypeDescriptor);
  }

  private Node rewriteArrayCastExpression(CastExpression castExpression) {
    Preconditions.checkArgument(
        castExpression.getCastTypeDescriptor() instanceof ArrayTypeDescriptor);
    ArrayTypeDescriptor arrayCastTypeDescriptor =
        (ArrayTypeDescriptor) castExpression.getCastTypeDescriptor();

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.createRaw(
            true,
            Visibility.PUBLIC,
            BootstrapType.ARRAYS.getDescriptor(),
            "$castTo",
            Lists.newArrayList(
                TypeDescriptors.get().javaLangObject,
                TypeDescriptors.get().javaLangObject,
                TypeDescriptors.get().primitiveInt),
            arrayCastTypeDescriptor,
            null,
            null,
            false);
    List<Expression> arguments = new ArrayList<>();
    arguments.add(castExpression.getExpression());
    arguments.add(arrayCastTypeDescriptor.getLeafTypeDescriptor().getRawTypeDescriptor());
    arguments.add(
        new NumberLiteral(
            TypeDescriptors.get().primitiveInt, arrayCastTypeDescriptor.getDimensions()));

    // Arrays.$castTo(expr, leafType, dimension);
    MethodCall castMethodCall =
        MethodCall.createRegularMethodCall(null, castToMethodDescriptor, arguments);
    // /**@type {}*/ ()
    return CastExpression.createRaw(castMethodCall, arrayCastTypeDescriptor);
  }
}
