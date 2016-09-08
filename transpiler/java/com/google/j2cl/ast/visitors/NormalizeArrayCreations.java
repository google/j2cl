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
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsTypeAnnotation;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Normalizes array creations. */
public class NormalizeArrayCreations extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteNewArray(NewArray newArray) {
      if (newArray.getArrayLiteral() != null) {
        return rewriteArrayInit(newArray);
      }
      return rewriteArrayCreate(newArray);
    }
  }

  /** We transform new Object[100][100]; to Arrays.$create([100, 100], Object); */
  private static Node rewriteArrayCreate(NewArray newArrayExpression) {
    Preconditions.checkArgument(newArrayExpression.getArrayLiteral() == null);
    MethodDescriptor arrayCreateMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setEnclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setMethodName("$create")
            .setParameterTypeDescriptors(
                Arrays.asList(
                    TypeDescriptors.getForArray(TypeDescriptors.get().primitiveInt, 1),
                    TypeDescriptors.get().javaLangObject))
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(
        new ArrayLiteral(
            TypeDescriptors.getForArray(TypeDescriptors.get().primitiveInt, 1),
            newArrayExpression.getDimensionExpressions()));
    // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
    // parameter type instead of the type parameter itself.
    TypeReference leafTypeReference =
        new TypeReference(newArrayExpression.getLeafTypeDescriptor().getRawTypeDescriptor());
    arguments.add(leafTypeReference);
    MethodCall arrayCreateMethodCall =
        MethodCall.Builder.from(arrayCreateMethodDescriptor).setArguments(arguments).build();
    return JsTypeAnnotation.createTypeAnnotation(
        arrayCreateMethodCall,
        TypeDescriptors.toNonNullable(newArrayExpression.getTypeDescriptor()));
  }

  /**
   * We transform new Object[][] {{object, object}, {object, object}} to Arrays.$init([[object,
   * object], [object, object]], Object, 2);
   */
  private static Node rewriteArrayInit(NewArray newArrayExpression) {
    Preconditions.checkArgument(newArrayExpression.getArrayLiteral() != null);
    int dimensionCount = newArrayExpression.getDimensionExpressions().size();
    MethodDescriptor arrayInitMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setEnclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setMethodName("$init")
            .setParameterTypeDescriptors(
                Arrays.asList(
                    TypeDescriptors.getForArray(TypeDescriptors.get().javaLangObject, 1),
                    TypeDescriptors.get().javaLangObject))
            .build();
    if (dimensionCount == 1) {
      // It's 1 dimensional.
      if (TypeDescriptors.get()
          .javaLangObject
          .equalsIgnoreNullability(newArrayExpression.getLeafTypeDescriptor())) {
        // And the leaf type is Object. All arrays are implicitly Array<Object> so leave out the
        // init.
        return newArrayExpression.getArrayLiteral();
      }
      // Number of dimensions defaults to 1 so we can leave that parameter out.

      List<Expression> arguments = new ArrayList<>();
      arguments.add(newArrayExpression.getArrayLiteral());
      // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
      // parameter type instead of the type parameter itself.
      TypeReference leafTypeReference =
          new TypeReference(newArrayExpression.getLeafTypeDescriptor().getRawTypeDescriptor());
      arguments.add(leafTypeReference);
      MethodCall arrayInitMethodCall =
          MethodCall.Builder.from(arrayInitMethodDescriptor).setArguments(arguments).build();
      return JsTypeAnnotation.createTypeAnnotation(
          arrayInitMethodCall,
          TypeDescriptors.toNonNullable(newArrayExpression.getTypeDescriptor()));
    } else {
      // It's multidimensional, make dimensions explicit.
      arrayInitMethodDescriptor =
          MethodDescriptor.Builder.from(arrayInitMethodDescriptor)
              .setParameterTypeDescriptors(
                  Iterables.concat(
                      arrayInitMethodDescriptor.getParameterTypeDescriptors(),
                      Arrays.asList(TypeDescriptors.get().primitiveInt)))
              .build();
      List<Expression> arguments = new ArrayList<>();
      arguments.add(newArrayExpression.getArrayLiteral());
      // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
      // parameter type instead of the type parameter itself.
      TypeReference leafTypeReference =
          new TypeReference(newArrayExpression.getLeafTypeDescriptor().getRawTypeDescriptor());
      arguments.add(leafTypeReference);
      arguments.add(new NumberLiteral(TypeDescriptors.get().primitiveInt, dimensionCount));
      MethodCall arrayInitMethodCall =
          MethodCall.Builder.from(arrayInitMethodDescriptor).setArguments(arguments).build();

      return JsTypeAnnotation.createTypeAnnotation(
          arrayInitMethodCall,
          TypeDescriptors.toNonNullable(newArrayExpression.getTypeDescriptor()));
    }
  }
}
