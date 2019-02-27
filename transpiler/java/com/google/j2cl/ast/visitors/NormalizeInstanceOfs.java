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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

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
              return BinaryExpression.newBuilder()
                  .setLeftOperand(subject)
                  .setOperator(BinaryOperator.NOT_EQUALS)
                  .setRightOperand(NullLiteral.get())
                  .build();
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
    JavaScriptConstructorReference javaScriptConstructorReference =
        checkTypeDescriptor.getMetadataConstructorReference();

    MethodDescriptor isInstanceMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(
                javaScriptConstructorReference
                    .getReferencedTypeDeclaration()
                    .toUnparameterizedTypeDescriptor())
            .setName("$isInstance")
            .setParameterTypeDescriptors(TypeDescriptors.get().javaLangObject)
            .setReturnTypeDescriptor(PrimitiveTypes.BOOLEAN)
            .build();

    // TypeName.$isInstance(expr);
    return MethodCall.Builder.from(isInstanceMethodDescriptor)
        .setQualifier(javaScriptConstructorReference)
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
