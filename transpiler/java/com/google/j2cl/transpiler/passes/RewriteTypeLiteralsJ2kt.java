/*
 * Copyright 2026 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PackageDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/**
 * Rewrites Java class literals to Kotlin KClass property calls (javaObjectType/javaPrimitiveType)
 * unless they are in annotations or explicitly assigned to KClass.
 */
public class RewriteTypeLiteralsJ2kt extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessMethod(Method method) {
            // Do not rewrite type literals inside annotation default values.
            // These need to be kotlin.reflect.KClass instances and not java.lang.Class.
            return method.getDefaultValue() == null;
          }

          @Override
          public Node rewriteTypeLiteral(TypeLiteral typeLiteral) {
            TypeDescriptor referencedType = typeLiteral.getReferencedTypeDescriptor();
            if (referencedType.isPrimitive()) {
              // int.class -> Int::class.javaPrimitiveType!!
              return FieldAccess.builderFrom(
                      getJavaTypeFieldDescriptor(
                          typeLiteral.getTypeDescriptor(), "javaPrimitiveType"))
                  .setQualifier(typeLiteral)
                  .build()
                  .postfixNotNullAssertion();
            } else {
              // String.class -> String::class.javaObjectType
              return FieldAccess.builderFrom(
                      getJavaTypeFieldDescriptor(typeLiteral.getTypeDescriptor(), "javaObjectType"))
                  .setQualifier(typeLiteral)
                  .build();
            }
          }
        });
  }

  private static FieldDescriptor getJavaTypeFieldDescriptor(TypeDescriptor type, String fieldName) {
    return FieldDescriptor.builder()
        .setEnclosingTypeDescriptor(
            TypeDeclaration.builder()
                .setPackage(PackageDeclaration.builder().setName("kotlin.reflect").build())
                .setClassComponents("KClass")
                .setKind(TypeDeclaration.Kind.CLASS)
                .build()
                .toDescriptor())
        .setTypeDescriptor(type)
        .setName(fieldName)
        .build();
  }
}
