/*
 * Copyright 2025 Google Inc.
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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isNumericPrimitive;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/**
 * Evaluates compile-time constants in constructs where the transpiler assumes they are.
 *
 * <p>These constructs include field initializers, case expressions, etc.
 */
public class EvaluateCompileTimeConstants extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteExpression(Expression expression) {
            Object parent = getParent();
            if (needsStaticEvaluation(parent, expression)) {
              return expression.getConstantValue();
            }
            return expression;
          }

          @Override
          public Node rewriteField(Field field) {
            Expression initializer = field.getInitializer();
            if (initializer != null && field.isCompileTimeConstant()) {
              TypeDescriptor typeDescriptor = field.getDescriptor().getTypeDescriptor();
              if (isNumericPrimitive(typeDescriptor)) {
                // Coerce the type of the constant to the field, since Java allows initializing
                // integral typed fields that are smaller than integer with an integer literal.
                initializer =
                    Literal.fromValue(
                        ((NumberLiteral) initializer.getConstantValue()).getValue(),
                        typeDescriptor);
              }
              return Field.Builder.from(field).setInitializer(initializer).build();
            }
            return field;
          }
        });
  }

  private static boolean needsStaticEvaluation(Object parent, Expression expression) {
    if (!expression.isCompileTimeConstant()) {
      return false;
    }
    if (parent instanceof SwitchCase) {
      // Case expressions need static evaluation.
      return true;
    }

    return isJsEnumCustomValue(parent, expression);
  }

  private static boolean isJsEnumCustomValue(Object parent, Expression expression) {
    // The custom value for a JsEnum is the first parameter in the instantiation that corresponds to
    // the declaration of the enum value.
    if (!(parent instanceof NewInstance newInstance)) {
      return false;
    }

    var enclosingTypeDescriptor = newInstance.getTarget().getEnclosingTypeDescriptor();
    return enclosingTypeDescriptor.isJsEnum()
        && enclosingTypeDescriptor.getJsEnumInfo().hasCustomValue()
        && newInstance.getArguments().get(0) == expression;
  }
}
