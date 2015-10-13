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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Visibility;

/**
 * Inserts a Strings.valueOf() operation when a non-string type is part of a "+" operation along
 * with the string type in a string conversion context.
 */
public class InsertStringConversionVisitor extends ConversionContextVisitor {

  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertStringConversionVisitor().run(compilationUnit);
  }

  public InsertStringConversionVisitor() {
    super(
        new ContextRewriter() {
          @Override
          public Expression rewriteStringContext(Expression operandExpression) {
            TypeDescriptor typeDescriptor = operandExpression.getTypeDescriptor();
            // If it's already a String, leave it alone.
            if (typeDescriptor == TypeDescriptors.get().javaLangString) {
              return operandExpression;
            }
            // Normally Java would box a primitive but we don't because JS already converts
            // primitives to String in the presence of a + operator.
            if (TypeDescriptors.isPrimitiveType(typeDescriptor)) {
              return operandExpression;
            }

            // At this point we're guaranteed to have in hand a non-string reference type (or
            // at least a boolean or double primitive that we can treat as a reference type).
            // So use a "String.valueOf()" method call on it.
            MethodDescriptor toStringMethodDescriptor =
                MethodDescriptor.create(
                    true, // static
                    Visibility.PUBLIC,
                    TypeDescriptors.get().javaLangString,
                    "valueOf",
                    false, // constructor
                    false, // native
                    TypeDescriptors.get().javaLangString,
                    Lists.newArrayList(TypeDescriptors.get().javaLangObject));

            return new MethodCall(
                null, toStringMethodDescriptor, Lists.newArrayList(operandExpression));
          }
        });
  }
}
