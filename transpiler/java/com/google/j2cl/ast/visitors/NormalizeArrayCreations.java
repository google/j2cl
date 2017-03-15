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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeReference;

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
      checkState(newArrayExpression.getDimensionExpressions().size() == 1);
      Expression dimensionExpression =
          Iterables.getOnlyElement(newArrayExpression.getDimensionExpressions());

      MethodDescriptor nativeArrayConstructor =
          MethodDescriptor.newBuilder()
              .setConstructor(true)
              .setJsInfo(JsInfo.RAW_CTOR)
              .setEnclosingClassTypeDescriptor(TypeDescriptors.NATIVE_ARRAY)
              .setParameterTypeDescriptors(dimensionExpression.getTypeDescriptor())
              .build();

      return NewInstance.Builder.from(nativeArrayConstructor)
          .setArguments(dimensionExpression)
          .build();
    }

    MethodDescriptor arrayCreateMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setName("$create")
            .setParameterTypeDescriptors(
                TypeDescriptors.getForArray(TypeDescriptors.get().primitiveInt, 1),
                TypeDescriptors.get().javaLangObject)
            .build();
    // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
    // parameter type instead of the type parameter itself.
    MethodCall arrayCreateMethodCall =
        MethodCall.Builder.from(arrayCreateMethodDescriptor)
            .setArguments(
                new ArrayLiteral(
                    TypeDescriptors.getForArray(TypeDescriptors.get().primitiveInt, 1),
                    newArrayExpression.getDimensionExpressions()),
                new TypeReference(
                    newArrayExpression.getLeafTypeDescriptor().getRawTypeDescriptor()))
            .build();
    return JsDocAnnotatedExpression.newBuilder()
        .setExpression(arrayCreateMethodCall)
        .setAnnotationType(TypeDescriptors.toNonNullable(newArrayExpression.getTypeDescriptor()))
        .build();
  }

  /**
   * We transform new Object[][] {{object, object}, {object, object}} to Arrays.$init([[object,
   * object], [object, object]], Object, 2);
   */
  private static Expression rewriteArrayInit(NewArray newArrayExpression) {
    checkArgument(newArrayExpression.getArrayLiteral() != null);

    if (newArrayExpression.getTypeDescriptor().isUntypedArray()) {
      checkState(newArrayExpression.getDimensionExpressions().size() == 1);
      return newArrayExpression.getArrayLiteral();
    }

    int dimensionCount = newArrayExpression.getDimensionExpressions().size();
    MethodDescriptor arrayInitMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setName("$init")
            .setParameterTypeDescriptors(
                TypeDescriptors.getForArray(TypeDescriptors.get().javaLangObject, 1),
                TypeDescriptors.get().javaLangObject)
            .build();

    if (dimensionCount == 1) {
      // Number of dimensions defaults to 1 so we can leave that parameter out.

      // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
      // parameter type instead of the type parameter itself.
      MethodCall arrayInitMethodCall =
          MethodCall.Builder.from(arrayInitMethodDescriptor)
              .setArguments(
                  newArrayExpression.getArrayLiteral(),
                  new TypeReference(
                      newArrayExpression.getLeafTypeDescriptor().getRawTypeDescriptor()))
              .build();
      return JsDocAnnotatedExpression.newBuilder()
          .setExpression(arrayInitMethodCall)
          .setAnnotationType(TypeDescriptors.toNonNullable(newArrayExpression.getTypeDescriptor()))
          .build();
    } else {
      // It's multidimensional, make dimensions explicit.
      arrayInitMethodDescriptor =
          MethodDescriptor.Builder.from(arrayInitMethodDescriptor)
              .addParameterTypeDescriptors(TypeDescriptors.get().primitiveInt)
              .build();

      // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
      // parameter type instead of the type parameter itself.
      MethodCall arrayInitMethodCall =
          MethodCall.Builder.from(arrayInitMethodDescriptor)
              .setArguments(
                  newArrayExpression.getArrayLiteral(),
                  new TypeReference(
                      newArrayExpression.getLeafTypeDescriptor().getRawTypeDescriptor()),
                  new NumberLiteral(TypeDescriptors.get().primitiveInt, dimensionCount))
              .build();

      return JsDocAnnotatedExpression.newBuilder()
          .setExpression(arrayInitMethodCall)
          .setAnnotationType(TypeDescriptors.toNonNullable(newArrayExpression.getTypeDescriptor()))
          .build();
    }
  }

}
