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
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
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
        AstUtils.getMetadataConstructorReference(checkTypeDescriptor);

    MethodDescriptor isInstanceMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(
                javaScriptConstructorReference
                    .getReferencedTypeDeclaration()
                    .toUnparamterizedTypeDescriptor())
            .setName("$isInstance")
            .setParameterTypeDescriptors(TypeDescriptors.get().javaLangObject)
            .setReturnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
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
    checkState(!leafTypeDescriptor.isTypeVariable() && !leafTypeDescriptor.isWildCardOrCapture());

    if (leafTypeDescriptor.isNative()) {
      return RuntimeMethods.createArraysMethodCall("$instanceIsOfNative", expression);
    }

    return RuntimeMethods.createArraysMethodCall(
        "$instanceIsOfType",
        expression,
        AstUtils.getMetadataConstructorReference(leafTypeDescriptor),
        NumberLiteral.of(checkTypeDescriptor.getDimensions()));
  }
}
