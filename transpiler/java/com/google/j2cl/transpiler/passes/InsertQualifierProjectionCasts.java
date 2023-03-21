/*
 * Copyright 2023 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;

/** Inserts projection casts for method-call qualifiers, necessary for Kotlin */
public final class InsertQualifierProjectionCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMemberReference(MemberReference memberReference) {
            Expression qualifier = memberReference.getQualifier();
            if (qualifier == null) {
              return memberReference;
            }

            TypeDescriptor qualifierTypeDescriptor = qualifier.getTypeDescriptor();
            if (!(qualifierTypeDescriptor instanceof DeclaredTypeDescriptor)) {
              return memberReference;
            }

            DeclaredTypeDescriptor declaredTypeDescriptor =
                (DeclaredTypeDescriptor) qualifierTypeDescriptor;
            TypeDeclaration typeDeclaration = declaredTypeDescriptor.getTypeDeclaration();

            // Exclude cast on recursive types, which would be invalid in Kotlin.
            if (typeDeclaration.hasRecursiveTypeBounds()) {
              return memberReference;
            }

            TypeDescriptor typeDescriptor = qualifier.getTypeDescriptor();
            TypeDescriptor projectedTypeDescriptor = projectTypeArgumentsUpperBound(typeDescriptor);
            if (typeDescriptor.equals(projectedTypeDescriptor)) {
              return memberReference;
            }

            return MemberReference.Builder.from(memberReference)
                .setQualifier(
                    CastExpression.newBuilder()
                        .setExpression(qualifier)
                        .setCastTypeDescriptor(projectedTypeDescriptor)
                        .build())
                .build();
          }
        });
  }

  private static TypeDescriptor projectTypeArgumentsUpperBound(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return DeclaredTypeDescriptor.Builder.from(declaredTypeDescriptor)
          .setTypeArgumentDescriptors(
              declaredTypeDescriptor.getTypeArgumentDescriptors().stream()
                  .map(InsertQualifierProjectionCasts::projectUpperBound)
                  .collect(toImmutableList()))
          .build();
    }

    return typeDescriptor;
  }

  private static TypeDescriptor projectUpperBound(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()
          && typeVariable.getLowerBoundTypeDescriptor() == null) {
        return typeVariable.getUpperBoundTypeDescriptor();
      }
    }
    return typeDescriptor;
  }
}
