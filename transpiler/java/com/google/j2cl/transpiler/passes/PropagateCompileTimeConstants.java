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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;
import javax.annotation.Nullable;

/** Propagates compile time constant fields. */
public class PropagateCompileTimeConstants extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    removeCompileTimeConstantFields(type);
    rewriteCompileTimeConstantFieldReferences(type);
  }

  /** Removes all compile constant fields and returns their values indexed by them. */
  private void removeCompileTimeConstantFields(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Field rewriteField(Field field) {
            if (field.getDescriptor().getConstantValue() != null) {
              // Remove the compile time constant initialization since the values are propagated
              // to all usage sites across compilation boundaries.
              // This is particularly done to remove the initialization of string compile time
              // constants which are rendered as globals and the current representation of string
              // literal initializers can no be used for initializing them.
              return null;
            }
            return field;
          }
        });
  }

  /** Replaces references to compile time constant with the corresponding literal. */
  private void rewriteCompileTimeConstantFieldReferences(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor target = fieldAccess.getTarget();
            Literal constantValue = target.getConstantValue();
            if (constantValue == null) {
              return fieldAccess;
            }
            // Qualifiers for static methods have already been normalized out.
            checkState(fieldAccess.getQualifier() == null);

            return constantValue;
          }
        });
  }
}
