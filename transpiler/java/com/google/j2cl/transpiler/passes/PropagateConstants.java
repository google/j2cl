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
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Propagates compile time constant fields. */
public class PropagateConstants extends LibraryNormalizationPass {

  @Override
  public void applyTo(Library library) {
    Map<FieldDescriptor, Literal> literalsByField = removeCompileTimeConstantFields(library);
    rewriteCompileTimeConstantFieldReferences(library, literalsByField);
  }

  /** Removes all string compile constant fields and returns their values indexed by them. */
  private Map<FieldDescriptor, Literal> removeCompileTimeConstantFields(Library library) {
    Map<FieldDescriptor, Literal> literalsByField = new HashMap<>();
    library.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Field rewriteField(Field field) {
            if (isCompileTimeConstant(field)) {
              checkState(field.isStatic());
              // We expect compile time constant to resolved at this stage so we can assume
              // initializer is a StringLiteral.
              literalsByField.put(field.getDescriptor(), (Literal) field.getInitializer());
              return null;
            }
            return field;
          }
        });
    return literalsByField;
  }

  /**
   * Replaces references to compile time constant fields of type String with a call to the
   * corresponding literal getter.
   */
  private void rewriteCompileTimeConstantFieldReferences(
      Library library, Map<FieldDescriptor, Literal> literalsByField) {
    library.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor target = fieldAccess.getTarget();
            Expression literal = literalsByField.get(target.getDeclarationDescriptor());
            if (literal == null) {
              return fieldAccess;
            }

            // Qualifiers for static methods have already been normalized out.
            checkState(fieldAccess.getQualifier() == null);
            return literal;
          }
        });
  }

  private static boolean isCompileTimeConstant(Field field) {
    return field.isCompileTimeConstant()
        // Consider final static fields that are initialized to literals to be compile time
        // constants. These might be driven from TypeLiterals or System.getProperty calls and it
        // ensures removal of clinits in some of the JRE classes.
        || (field.getDescriptor().isFinal()
            && field.isStatic()
            && (field.getInitializer() instanceof TypeLiteral
                || field.getInitializer() instanceof StringLiteral));
  }
}
