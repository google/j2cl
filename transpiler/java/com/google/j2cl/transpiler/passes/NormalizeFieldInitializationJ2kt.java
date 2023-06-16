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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/** Initializes non-final, nullable fields with explicit default value. */
public class NormalizeFieldInitializationJ2kt extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Member rewriteField(Field field) {
            FieldDescriptor fieldDescriptor = field.getDescriptor();

            // Final fields without initializer should be initialized on all constructor paths.
            if (fieldDescriptor.isFinal() || field.hasInitializer()) {
              return field;
            }

            TypeDescriptor typeDescriptor = fieldDescriptor.getTypeDescriptor();

            // In Java, non-final primitive fields or reference fields which are nullable
            // do not need to be initialized on all constructor paths, as they are implicitly
            // initialized with fallback value.
            // This is not the case in Kotlin, so we make it explicit.
            if (typeDescriptor.isPrimitive() || typeDescriptor.isNullable()) {
              return fieldWithDefaultInitializer(field);
            }

            // Other fields are assumed to be initialized on all constructor paths.
            return field;
          }
        });
  }

  private static Field fieldWithDefaultInitializer(Field field) {
    TypeDescriptor typeDescriptor = field.getDescriptor().getTypeDescriptor();
    Expression initializer = typeDescriptor.getDefaultValue();
    return Field.Builder.from(field).setInitializer(initializer).build();
  }
}
