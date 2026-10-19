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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import javax.annotation.Nullable;

/**
 * Inlines boxed type {@code TYPE} field accesses (e.g. {@code Integer.TYPE}) to primitive type
 * literals (e.g. {@code int.class}) and removes the {@code TYPE} field declarations.
 */
public class OptimizeBoxedTypeFields extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Field rewriteField(Field field) {
            if (isBoxedTypeField(field.getDescriptor())) {
              return null;
            }
            return field;
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor target = fieldAccess.getTarget();
            if (!isBoxedTypeField(target)) {
              return fieldAccess;
            }
            // Qualifiers for static members have already been normalized out.
            checkState(fieldAccess.getQualifier() == null);
            return new TypeLiteral(
                fieldAccess.getSourcePosition(),
                target.getEnclosingTypeDescriptor().toUnboxedType());
          }
        });
  }

  private static boolean isBoxedTypeField(FieldDescriptor fieldDescriptor) {
    DeclaredTypeDescriptor enclosingType = fieldDescriptor.getEnclosingTypeDescriptor();
    return fieldDescriptor.getName().equals("TYPE")
        && (TypeDescriptors.isBoxedType(enclosingType)
            || TypeDescriptors.isJavaLangVoid(enclosingType));
  }
}
