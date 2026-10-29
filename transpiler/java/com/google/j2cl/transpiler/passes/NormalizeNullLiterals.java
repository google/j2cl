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

import static com.google.j2cl.transpiler.ast.AstUtils.isBoxableJsEnumType;

import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Replaces all null literals with null literals that have the exact type required in the construct
 * they appear.
 */
public class NormalizeNullLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                if (expression instanceof NullLiteral) {
                  if (isBoxableJsEnumType(inferredTypeDescriptor)
                      && !isBoxableJsEnumType(declaredTypeDescriptor)) {
                    // If the declared type is not a JsEnum but the inferred type is, as in
                    // listOfEnums.add(null), type it as j.l.Object (the direct superclass for a
                    // boxed enum) to avoid inserting a redundant boxing conversion.
                    return TypeDescriptors.get().javaLangObject.getNullValue();
                  }
                  // Declare the null literal to be the inferred type.
                  // Note that it is typed as JsEnum if the declared type is a JsEnum to avoid
                  // inserting a redundant unboxing conversion.
                  return inferredTypeDescriptor.getNullValue();
                }
                return expression;
              }

              @Override
              public Expression rewriteCastContext(CastExpression castExpression) {
                TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
                if (castExpression.getExpression() instanceof NullLiteral
                    && isBoxableJsEnumType(castTypeDescriptor)) {
                  // Casting null always succeeds, replace the cast with a null literal of the
                  // JsEnum type to avoid inserting a redundant unboxing conversion.
                  return castTypeDescriptor.getNullValue();
                }
                return castExpression;
              }
            }));
  }
}
