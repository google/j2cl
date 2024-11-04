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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.List;

/** Normalizes array creations. */
public class NormalizeArrayCreations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getInitializer() instanceof ArrayLiteral) {
              return implementArrayCreationFromArrayLiteral(newArray);
            } else if (newArray.getInitializer() != null) {
              return implementArrayCreationWithInitializerFunction(newArray);
            } else {
              return implementArrayCreationWithDimensions(newArray);
            }
          }
        });
  }

  /** We transform new Object[100][100]; to Arrays.$create([100, 100], Object); */
  private static Expression implementArrayCreationWithDimensions(NewArray newArrayExpression) {
    checkArgument(!(newArrayExpression.getInitializer() instanceof ArrayLiteral));

    if (AstUtils.shouldUseUntypedArray(newArrayExpression.getTypeDescriptor())) {
      if (newArrayExpression.getDimensionExpressions().size() == 1) {
        Expression dimensionExpression =
            Iterables.getOnlyElement(newArrayExpression.getDimensionExpressions());

        MethodDescriptor nativeArrayConstructor =
            MethodDescriptor.newBuilder()
                .setConstructor(true)
                .setOriginalJsInfo(JsInfo.RAW_CTOR)
                .setEnclosingTypeDescriptor(TypeDescriptors.get().nativeArray)
                .setParameterTypeDescriptors(dimensionExpression.getTypeDescriptor())
                .build();

        return NewInstance.Builder.from(nativeArrayConstructor)
            .setArguments(dimensionExpression)
            .build();
      }

      return createNonNullableAnnotation(
          RuntimeMethods.createArraysMethodCall(
              "$createNative",
              new ArrayLiteral(
                  TypeDescriptors.get().javaLangObjectArray,
                  newArrayExpression.getDimensionExpressions())),
          newArrayExpression.getTypeDescriptor());
    }

    TypeDescriptor leafTypeDescriptor = newArrayExpression.getLeafTypeDescriptor();
    return createNonNullableAnnotation(
        RuntimeMethods.createArraysMethodCall(
            "$create",
            new ArrayLiteral(
                TypeDescriptors.get().javaLangObjectArray,
                newArrayExpression.getDimensionExpressions()),
            leafTypeDescriptor.getMetadataConstructorReference()),
        newArrayExpression.getTypeDescriptor());
  }

  /** Create Kotlin style initializations using function. */
  private static Expression implementArrayCreationWithInitializerFunction(
      NewArray newArrayExpression) {
    checkArgument(
        newArrayExpression.getInitializer() != null
            && !(newArrayExpression.getInitializer() instanceof ArrayLiteral));

    Expression dimensionLengthExpression =
        checkNotNull(newArrayExpression.getDimensionExpressions().get(0));

    if (AstUtils.shouldUseUntypedArray(newArrayExpression.getTypeDescriptor())) {
      return createNonNullableAnnotation(
          RuntimeMethods.createArraysMethodCall(
              "$createNativeWithInitializer",
              Lists.newArrayList(
                  dimensionLengthExpression, // currentDimensionLength
                  newArrayExpression.getInitializer() // initializer
                  )),
          newArrayExpression.getTypeDescriptor());
    }

    List<Expression> arguments =
        Lists.newArrayList(
            dimensionLengthExpression, // currentDimensionLength
            newArrayExpression
                .getLeafTypeDescriptor()
                .getMetadataConstructorReference(), // leaf type
            newArrayExpression.getInitializer() // initializer
            );

    int numDimensions = newArrayExpression.getDimensionExpressions().size();
    if (numDimensions > 1) {
      arguments.add(NumberLiteral.fromInt(numDimensions));
    }

    return createNonNullableAnnotation(
        RuntimeMethods.createArraysMethodCall("$createWithInitializer", arguments),
        newArrayExpression.getTypeDescriptor());
  }

  /**
   * We transform new Object[][] {{object, object}, {object, object}} to Arrays.$stampType([[object,
   * object], [object, object]], Object, 2);
   */
  private static Expression implementArrayCreationFromArrayLiteral(NewArray newArrayExpression) {
    Expression initializer = newArrayExpression.getInitializer();
    checkArgument(initializer instanceof ArrayLiteral);

    if (AstUtils.shouldUseUntypedArray(newArrayExpression.getTypeDescriptor())) {
      return initializer;
    }

    ArrayTypeDescriptor arrayTypeDescriptor = newArrayExpression.getTypeDescriptor();
    return createNonNullableAnnotation(
        RuntimeMethods.createArraysStampTypeMethodCall(initializer, arrayTypeDescriptor),
        arrayTypeDescriptor);
  }

  /**
   * Annotates the expression with the non nullable type corresponding to {@code typeDescriptor}.
   */
  private static Expression createNonNullableAnnotation(
      Expression expression, TypeDescriptor typeDescriptor) {
    return JsDocCastExpression.newBuilder()
        .setExpression(expression)
        .setCastTypeDescriptor(typeDescriptor.toNonNullable())
        .build();
  }
}
