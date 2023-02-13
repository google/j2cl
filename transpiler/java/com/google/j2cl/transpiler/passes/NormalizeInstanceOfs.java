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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.ast.MethodDescriptor.IS_INSTANCE_METHOD_NAME;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Replaces instanceof expression with corresponding $isInstance method call. */
public class NormalizeInstanceOfs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInstanceOfExpression(InstanceOfExpression expression) {
            Expression subject = expression.getExpression();
            // Replace trivial instanceof expression with a null check.
            if (subject.getTypeDescriptor().isAssignableTo(expression.getTestTypeDescriptor())) {
              return subject.infixNotEqualsNull();
            }

            if (expression.getTestTypeDescriptor().isArray()) {
              return rewriteArrayInstanceOfExpression(expression);
            } else {
              return rewriteRegularInstanceOfExpression(expression);
            }
          }
        });
  }

  private static Node rewriteRegularInstanceOfExpression(
      InstanceOfExpression instanceOfExpression) {
    TypeDescriptor checkTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();

    MethodDescriptor isInstanceMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setOriginalJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(
                checkTypeDescriptor.getMetadataTypeDeclaration().toUnparameterizedTypeDescriptor())
            .setName(IS_INSTANCE_METHOD_NAME)
            .setParameterTypeDescriptors(TypeDescriptors.get().javaLangObject)
            .setReturnTypeDescriptor(PrimitiveTypes.BOOLEAN)
            .build();

    // TypeName.$isInstance(expr);
    return MethodCall.Builder.from(isInstanceMethodDescriptor)
        .setArguments(instanceOfExpression.getExpression())
        .build();
  }

  private static Node rewriteArrayInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
    Expression expression = instanceOfExpression.getExpression();
    ArrayTypeDescriptor checkTypeDescriptor =
        (ArrayTypeDescriptor) instanceOfExpression.getTestTypeDescriptor();
    TypeDescriptor leafTypeDescriptor = checkTypeDescriptor.getLeafTypeDescriptor();
    checkState(
        leafTypeDescriptor instanceof DeclaredTypeDescriptor
            || leafTypeDescriptor instanceof PrimitiveTypeDescriptor);

    if (leafTypeDescriptor.isNative()) {
      return RuntimeMethods.createArraysMethodCall("$instanceIsOfNative", expression);
    }

    return RuntimeMethods.createArraysMethodCall(
        "$instanceIsOfType",
        expression,
        leafTypeDescriptor.getMetadataConstructorReference(),
        NumberLiteral.fromInt(checkTypeDescriptor.getDimensions()));
  }
}
