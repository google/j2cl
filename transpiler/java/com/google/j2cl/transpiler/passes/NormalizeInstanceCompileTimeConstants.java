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
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Type;

/** Converts instance compile time constants to static compile time constant. */
public class NormalizeInstanceCompileTimeConstants extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Member rewriteField(Field field) {
            if (isInstanceCompileTimeConstant(field.getDescriptor())) {
              return Field.Builder.from(field)
                  .setDescriptor(toStatic(field.getDescriptor()))
                  .build();
            }
            return field;
          }

          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            if (isInstanceCompileTimeConstant(fieldAccess.getTarget())) {
              return FieldAccess.Builder.from(fieldAccess)
                  .setTarget(toStatic(fieldAccess.getTarget()))
                  .build();
            }
            return fieldAccess;
          }

          private boolean isInstanceCompileTimeConstant(FieldDescriptor fieldDescriptor) {
            return !fieldDescriptor.isStatic() && fieldDescriptor.isCompileTimeConstant();
          }

          private FieldDescriptor toStatic(FieldDescriptor fieldDescriptor) {
            return fieldDescriptor.transform(builder -> builder.setStatic(true));
          }
        });
  }
}
