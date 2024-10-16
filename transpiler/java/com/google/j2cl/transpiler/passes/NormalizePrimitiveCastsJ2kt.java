/*
 * Copyright 2021 Google Inc.
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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveChar;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveFloatOrDouble;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveInt;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/** Replaces cast expression on primitive types with corresponding Kotlin cast method call. */
public class NormalizePrimitiveCastsJ2kt extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(CastExpression castExpression) {
            Expression fromExpression = castExpression.getExpression();
            TypeDescriptor fromType = fromExpression.getTypeDescriptor();
            TypeDescriptor toType = castExpression.getTypeDescriptor();
            if (!toType.isPrimitive() || !fromType.isPrimitive()) {
              return castExpression;
            }

            PrimitiveTypeDescriptor fromPrimitiveType = (PrimitiveTypeDescriptor) fromType;
            PrimitiveTypeDescriptor toPrimitiveType = (PrimitiveTypeDescriptor) toType;

            // Skip conversion if not needed.
            if (fromPrimitiveType.equals(toPrimitiveType)) {
              return fromExpression;
            }

            // Primitive char needs to be converted first to int through ".code".
            if (isPrimitiveChar(fromPrimitiveType)) {
              fromExpression = convertCharCode(fromExpression);
              fromPrimitiveType = PrimitiveTypes.INT;
            }

            // Conversion to char must go through int.
            if (isPrimitiveChar(toPrimitiveType) && !isPrimitiveInt(fromPrimitiveType)) {
              fromExpression = convertTo(fromExpression, PrimitiveTypes.INT);
              fromPrimitiveType = PrimitiveTypes.INT;
            }

            // Conversion from float/double to a type that is narrower than int must go through int.
            if (isPrimitiveFloatOrDouble(fromPrimitiveType)
                && PrimitiveTypes.INT.isWiderThan(toPrimitiveType)) {
              fromExpression = convertTo(fromExpression, PrimitiveTypes.INT);
              fromPrimitiveType = PrimitiveTypes.INT;
            }

            // Apply final conversion if needed.
            return fromPrimitiveType.equals(toPrimitiveType)
                ? fromExpression
                : convertTo(fromExpression, toPrimitiveType);
          }
        });
  }

  private static final DeclaredTypeDescriptor KOTLIN_BASIC_TYPE =
      TypeDeclaration.newBuilder()
          .setKind(Kind.CLASS)
          .setQualifiedSourceName("j2kt.BasicType")
          .build()
          .toDescriptor();

  private static Expression convertCharCode(Expression expression) {
    FieldDescriptor castToFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(KOTLIN_BASIC_TYPE)
            .setName("code")
            .setTypeDescriptor(PrimitiveTypes.INT)
            .build();

    return FieldAccess.Builder.from(castToFieldDescriptor).setQualifier(expression).build();
  }

  private static Expression convertTo(
      Expression expression, PrimitiveTypeDescriptor primitiveType) {
    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(KOTLIN_BASIC_TYPE)
            .setName("to" + LOWER_CAMEL.to(UPPER_CAMEL, primitiveType.getSimpleSourceName()))
            .setReturnTypeDescriptor(primitiveType)
            .build();

    return MethodCall.Builder.from(castToMethodDescriptor).setQualifier(expression).build();
  }
}
