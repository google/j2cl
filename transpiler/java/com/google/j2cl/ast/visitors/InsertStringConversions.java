/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Inserts a Strings.valueOf() operation when a non-string type is part of a "+" operation along
 * with the string type in a string conversion context.
 */
public class InsertStringConversions extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new InsertStringConversions());
  }

  public InsertStringConversions() {
    super(
        new ContextRewriter() {
          @Override
          public Expression rewriteStringContext(
              Expression operandExpression, Expression otherOperandExpression) {
            TypeDescriptor typeDescriptor = operandExpression.getTypeDescriptor();
            if (typeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().javaLangString)) {
              // If it's a string, and it is not null or the otherOperandExpression is not null,
              // leave it alone.
              if (AstUtils.isNonNullString(operandExpression)
                  || AstUtils.isNonNullString(otherOperandExpression)) {
                return operandExpression;
              } else {
                // Otherwise, add an empty string at the front to make sure JS sees it as a
                // string operation.
                return new BinaryExpression(
                    TypeDescriptors.get().javaLangString,
                    StringLiteral.fromPlainText(""),
                    BinaryOperator.PLUS,
                    operandExpression);
              }
            }
            // Normally Java would box a primitive but we don't because JS already converts
            // primitives to String in the presence of a + operator. We make an exception for 'char'
            // since Java converts char to the matching String glyph and JS converts it into a
            // number String.
            if (TypeDescriptors.isNonVoidPrimitiveType(typeDescriptor)
                && !typeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveChar)) {
              return operandExpression;
            }

            // Convert char to Character so that Character.toString() will be called and thus Java's
            // semantic around char String conversion will be honored.
            if (typeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveChar)) {
              operandExpression = AstUtils.box(operandExpression);
            }

            // At this point we're guaranteed to have in hand a reference type (or
            // at least a boolean or double primitive that we can treat as a reference type).
            // So use a "String.valueOf()" method call on it.
            return MethodCall.createMethodCall(
                null, AstUtils.createStringValueOfMethodDescriptor(), operandExpression);
          }
        });
  }
}
