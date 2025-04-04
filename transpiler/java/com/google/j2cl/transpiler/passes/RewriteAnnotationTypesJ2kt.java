/*
 * Copyright 2024 Google Inc.
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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangClass;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PackageDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;

/**
 * Rewrite annotation types in annotation classes for Kotlin.
 *
 * <p>All occurrences of {@code java.lang.Class} are rewritten to {@code kotlin.reflect.KClass},
 * which is required in Kotlin.
 */
public class RewriteAnnotationTypesJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (!methodDescriptor.getEnclosingTypeDescriptor().isAnnotation()) {
              return method;
            }

            if (methodDescriptor.isStatic()) {
              return method;
            }

            return Method.Builder.from(method)
                .setMethodDescriptor(
                    MethodDescriptor.Builder.from(methodDescriptor)
                        .setReturnTypeDescriptor(
                            rewriteAnnotationTypeDescriptor(
                                methodDescriptor.getReturnTypeDescriptor()))
                        .build())
                .build();
          }
        });
  }

  private static TypeDescriptor rewriteAnnotationTypeDescriptor(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      if (isJavaLangClass(declaredTypeDescriptor)) {
        return TypeDeclaration.Builder.from(declaredTypeDescriptor.getTypeDeclaration())
            .setPackage(PackageDeclaration.newBuilder().setName("kotlin.reflect").build())
            .setClassComponents("KClass")
            .build()
            .toDescriptor()
            .toNonNullable()
            .withTypeArguments(
                declaredTypeDescriptor.getTypeArgumentDescriptors().stream()
                    .map(it -> rewriteAnnotationTypeDescriptor(it))
                    .collect(ImmutableList.toImmutableList()));
      }
    } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return arrayTypeDescriptor.withComponentTypeDescriptor(
          rewriteAnnotationTypeDescriptor(arrayTypeDescriptor.getComponentTypeDescriptor()));
    } else if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcard()) {
        return typeVariable.withRewrittenBounds(it -> rewriteAnnotationTypeDescriptor(it));
      }
    }

    return typeDescriptor;
  }
}
