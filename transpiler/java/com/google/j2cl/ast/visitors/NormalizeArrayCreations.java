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
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import java.util.List;

/** Normalizes array creations. */
public class NormalizeArrayCreations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewArray(NewArray newArray) {
            if (newArray.getArrayLiteral() != null) {
              return rewriteArrayInit(newArray);
            }
            return rewriteArrayCreate(newArray);
          }
        });
  }

  /** We transform new Object[100][100]; to Arrays.$create([100, 100], Object); */
  private static Expression rewriteArrayCreate(NewArray newArrayExpression) {
    checkArgument(newArrayExpression.getArrayLiteral() == null);

    if (newArrayExpression.getTypeDescriptor().isUntypedArray()) {
      if (newArrayExpression.getDimensionExpressions().size() == 1) {
        Expression dimensionExpression =
            Iterables.getOnlyElement(newArrayExpression.getDimensionExpressions());

        MethodDescriptor nativeArrayConstructor =
            MethodDescriptor.newBuilder()
                .setConstructor(true)
                .setJsInfo(JsInfo.RAW_CTOR)
                .setEnclosingTypeDescriptor(TypeDescriptors.get().nativeArray)
                .setParameterTypeDescriptors(dimensionExpression.getTypeDescriptor())
                .build();

        return NewInstance.Builder.from(nativeArrayConstructor)
            .setArguments(dimensionExpression)
            .build();
      }

      return createNonNullableAnnotation(
          AstUtils.createArraysMethodCall(
              "$createNative",
              new ArrayLiteral(
                  TypeDescriptors.getForArray(TypeDescriptors.get().primitiveInt, 1),
                  newArrayExpression.getDimensionExpressions())),
          newArrayExpression.getTypeDescriptor());
    }

    TypeDescriptor leafTypeDescriptor = newArrayExpression.getLeafTypeDescriptor();
    return createNonNullableAnnotation(
        AstUtils.createArraysMethodCall(
            "$create",
            new ArrayLiteral(
                TypeDescriptors.getForArray(TypeDescriptors.get().primitiveInt, 1),
                newArrayExpression.getDimensionExpressions()),
            AstUtils.getMetadataConstructorReference(leafTypeDescriptor)),
        newArrayExpression.getTypeDescriptor());
  }

  /**
   * We transform new Object[][] {{object, object}, {object, object}} to Arrays.$init([[object,
   * object], [object, object]], Object, 2);
   */
  private static Expression rewriteArrayInit(NewArray newArrayExpression) {
    checkArgument(newArrayExpression.getArrayLiteral() != null);

    if (newArrayExpression.getTypeDescriptor().isUntypedArray()) {
      return newArrayExpression.getArrayLiteral();
    }

    TypeDescriptor leafTypeDescriptor = newArrayExpression.getLeafTypeDescriptor();
    List<Expression> arguments =
        Lists.newArrayList(
            newArrayExpression.getArrayLiteral(),
            AstUtils.getMetadataConstructorReference(leafTypeDescriptor));

    int dimensionCount = newArrayExpression.getDimensionExpressions().size();
    if (dimensionCount > 1) {
      arguments.add(new NumberLiteral(TypeDescriptors.get().primitiveInt, dimensionCount));
    }

    return createNonNullableAnnotation(
        AstUtils.createArraysMethodCall("$init", arguments),
        newArrayExpression.getTypeDescriptor());
  }

  /**
   * Annotates the expression with the non nullable type corresponding to {@code typeDescriptor}.
   */
  private static Expression createNonNullableAnnotation(
      Expression expression, TypeDescriptor typeDescriptor) {
    return JsDocAnnotatedExpression.newBuilder()
        .setExpression(expression)
        .setAnnotationType(TypeDescriptors.toNonNullable(typeDescriptor))
        .build();
  }
}
