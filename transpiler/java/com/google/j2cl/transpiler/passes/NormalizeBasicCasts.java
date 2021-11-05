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

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;

/** Replaces cast expression on primitive &amp; boxed types with corresponding cast method call. */
public class NormalizeBasicCasts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(CastExpression castExpression) {
            TypeDescriptor castToType = castExpression.getTypeDescriptor();
            Expression innerExpression = castExpression.getExpression();
            TypeDescriptor castFromType = innerExpression.getTypeDescriptor();
            if (isBasicType(castToType) && isBasicType(castFromType)) {
              TypeDescriptor castToPrimitiveType = castToType.toUnboxedType();
              TypeDescriptor castFromPrimitiveType = castFromType.toUnboxedType();
              // In Kotlin, chars are 'cast' by getting their character code, which is an Int field.
              if (TypeDescriptors.isPrimitiveChar(castFromPrimitiveType)) {
                innerExpression =
                    implementCharPrimitiveCast(castToPrimitiveType, innerExpression, CODE);
                if (TypeDescriptors.isPrimitiveInt(castToPrimitiveType)) {
                  return innerExpression;
                }
              }
              String castMethodName = castMethodNames.get(castToPrimitiveType);
              if (castMethodName == null) {
                // In Kotlin, there is no boolean cast so this should have been removed.
                if (TypeDescriptors.isPrimitiveBoolean(castToPrimitiveType)) {
                  return innerExpression;
                }
                // TODO(dpo): this should not happen, what's the appropriate behavior if it does?
                return castExpression;
              }
              return implementNumericPrimitiveCast(
                  castToPrimitiveType, innerExpression, castMethodName);
            }
            return castExpression;
          }
        });
  }

  private static final String CODE = "code";

  private static final ImmutableMap<TypeDescriptor, String> castMethodNames =
      ImmutableMap.<TypeDescriptor, String>builder()
          .put(PrimitiveTypes.BYTE, "toByte")
          .put(PrimitiveTypes.SHORT, "toShort")
          .put(PrimitiveTypes.CHAR, "toChar")
          .put(PrimitiveTypes.INT, "toInt")
          .put(PrimitiveTypes.LONG, "toLong")
          .put(PrimitiveTypes.FLOAT, "toFloat")
          .put(PrimitiveTypes.DOUBLE, "toDouble")
          .build();

  private static boolean isBasicType(TypeDescriptor type) {
    return type.isPrimitive() || TypeDescriptors.isBoxedType(type);
  }

  private static Expression implementNumericPrimitiveCast(
      TypeDescriptor castTypeDescriptor, Expression expression, String method) {

    MethodDescriptor castToMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(BootstrapType.CASTS.getDescriptor())
            .setName(method)
            .setReturnTypeDescriptor(castTypeDescriptor)
            .build();

    // expr.toBasicTypeName();
    return MethodCall.Builder.from(castToMethodDescriptor).setQualifier(expression).build();
  }

  private static Expression implementCharPrimitiveCast(
      TypeDescriptor castTypeDescriptor, Expression expression, String field) {

    FieldDescriptor castToFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(BootstrapType.CASTS.getDescriptor())
            .setName(field)
            .setTypeDescriptor(castTypeDescriptor)
            .build();

    // expr.code;
    return FieldAccess.Builder.from(castToFieldDescriptor).setQualifier(expression).build();
  }
}
