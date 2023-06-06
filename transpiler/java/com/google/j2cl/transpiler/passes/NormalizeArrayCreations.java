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
              return rewriteArrayInit(newArray);
            } else if (newArray.getInitializer() != null) {
              return rewriteArrayWithInitializer(newArray);
            } else {
              return rewriteArrayCreate(newArray);
            }
          }
        });
  }

  /** We transform new Object[100][100]; to Arrays.$create([100, 100], Object); */
  private static Expression rewriteArrayCreate(NewArray newArrayExpression) {
    checkArgument(!(newArrayExpression.getInitializer() instanceof ArrayLiteral));

    if (newArrayExpression.getTypeDescriptor().isUntypedArray()) {
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

  private static Expression rewriteArrayWithInitializer(NewArray newArrayExpression) {
    checkArgument(
        newArrayExpression.getInitializer() != null
            && !(newArrayExpression.getInitializer() instanceof ArrayLiteral));

    Expression dimensionLengthExpression =
        checkNotNull(newArrayExpression.getDimensionExpressions().get(0));

    if (newArrayExpression.getTypeDescriptor().isUntypedArray()) {
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
   * We transform new Object[][] {{object, object}, {object, object}} to Arrays.$init([[object,
   * object], [object, object]], Object, 2);
   */
  private static Expression rewriteArrayInit(NewArray newArrayExpression) {
    checkArgument(newArrayExpression.getInitializer() instanceof ArrayLiteral);

    if (newArrayExpression.getTypeDescriptor().isUntypedArray()) {
      return newArrayExpression.getInitializer();
    }

    TypeDescriptor leafTypeDescriptor = newArrayExpression.getLeafTypeDescriptor();
    List<Expression> arguments =
        Lists.newArrayList(
            newArrayExpression.getInitializer(),
            leafTypeDescriptor.getMetadataConstructorReference());

    int dimensionCount = newArrayExpression.getDimensionExpressions().size();
    if (dimensionCount > 1) {
      arguments.add(NumberLiteral.fromInt(dimensionCount));
    }

    return createNonNullableAnnotation(
        RuntimeMethods.createArraysMethodCall("$init", arguments),
        newArrayExpression.getTypeDescriptor());
  }

  /**
   * Annotates the expression with the non nullable type corresponding to {@code typeDescriptor}.
   */
  private static Expression createNonNullableAnnotation(
      Expression expression, TypeDescriptor typeDescriptor) {
    return JsDocCastExpression.newBuilder()
        .setExpression(expression)
        .setCastType(typeDescriptor.toNonNullable())
        .build();
  }
}
