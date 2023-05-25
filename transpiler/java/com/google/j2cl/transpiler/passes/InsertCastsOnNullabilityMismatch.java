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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/** Inserts casts in places where necessary due to nullability differences in type arguments. */
public final class InsertCastsOnNullabilityMismatch extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              // Don't insert cast for member qualifiers, as their type is inferred.
              @Override
              public Expression rewriteMemberQualifierContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                return expression;
              }

              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor actualTypeDescriptor,
                  Expression expression) {
                return needsCast(expression.getTypeDescriptor(), project(inferredTypeDescriptor))
                    ? CastExpression.newBuilder()
                        .setExpression(expression)
                        .setCastTypeDescriptor(project(inferredTypeDescriptor))
                        .build()
                    : expression;
              }
            }));
  }

  private static boolean needsCast(TypeDescriptor from, TypeDescriptor to) {
    if (!to.isDenotable() || TypeDescriptors.isJavaLangVoid(to)) {
      return false;
    }

    if (from.isNullable() && !to.isNullable()) {
      return true;
    }

    return typeArgumentsNeedsCast(from, to);
  }

  private static boolean typeArgumentsNeedsCast(TypeDescriptor from, TypeDescriptor to) {
    return Streams.zip(
            getTypeArgumentDescriptors(from).stream(),
            getTypeArgumentDescriptors(to).stream(),
            InsertCastsOnNullabilityMismatch::typeArgumentNeedsCast)
        .anyMatch(Boolean::booleanValue);
  }

  private static boolean typeArgumentNeedsCast(TypeDescriptor from, TypeDescriptor to) {
    // Insert explicit cast from wildcard to non-wildcard, since in some cases wildcard type
    // arguments are inferred as non-wildcard in the AST.
    if (isWildcard(from) && !isWildcard(to)) {
      return true;
    }

    return from.isNullable() != to.isNullable() || typeArgumentsNeedsCast(from, to);
  }

  private static TypeDescriptor project(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()) {
        TypeDescriptor projected;
        TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
        if (lowerBound != null) {
          projected = project(lowerBound);
        } else {
          TypeDescriptor upperBound = typeVariable.getUpperBoundTypeDescriptor();
          projected = project(upperBound);
        }
        if (typeVariable.isNullable()) {
          projected = projected.toNullable();
        }
        return projected;
      }
    }

    return typeDescriptor;
  }

  private static boolean isWildcard(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      return typeVariable.isWildcardOrCapture();
    }

    return false;
  }

  private static ImmutableList<TypeDescriptor> getTypeArgumentDescriptors(
      TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return declaredTypeDescriptor.getTypeArgumentDescriptors();
    }

    if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return ImmutableList.of(arrayTypeDescriptor.getComponentTypeDescriptor());
    }

    return ImmutableList.of();
  }
}
