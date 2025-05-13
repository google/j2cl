/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/**
 * Inserts casts on type literals to match original Java type in Kotlin code.
 *
 * <p>Casts are necessary for:
 *
 * <ul>
 *   <li>type literals of generic types, like {@code List.class}, because in Java its type is {@code
 *       Class<List>} and in Kotlin it's {@code Class<List<*>>},
 *   <li>non-primitive arrays, because in Java its type is {@code Class<String[]} and in Kotlin
 *       component type is erased and is {@code Array<*>}.
 * </ul>
 */
public class InsertCastsForTypeLiteralsJ2kt extends AbstractJ2ktNormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteTypeLiteral(TypeLiteral typeLiteral) {
            TypeDescriptor referencedTypeDescriptor = typeLiteral.getReferencedTypeDescriptor();
            if (!needsCast(referencedTypeDescriptor)) {
              return typeLiteral;
            }

            return CastExpression.newBuilder()
                .setExpression(typeLiteral)
                .setCastTypeDescriptor(
                    TypeDescriptors.get()
                        .javaLangClass
                        .toNonNullable()
                        .withTypeArguments(ImmutableList.of(referencedTypeDescriptor)))
                .build();
          }
        });
  }

  private static boolean needsCast(TypeDescriptor referencedTypeDescriptor) {
    if (referencedTypeDescriptor instanceof DeclaredTypeDescriptor declaredTypeDescriptor) {
      return !declaredTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors().isEmpty();
    } else if (referencedTypeDescriptor instanceof ArrayTypeDescriptor arrayTypeDescriptor) {
      return !arrayTypeDescriptor.getComponentTypeDescriptor().isPrimitive();
    } else {
      return false;
    }
  }
}
