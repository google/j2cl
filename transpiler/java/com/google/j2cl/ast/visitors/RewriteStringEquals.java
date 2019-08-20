/*
 * Copyright 2019 Google Inc.
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

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/** Rewrites String.equals to == when called on a non null string. */
public class RewriteStringEquals extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().equals(getStringEquals())
                || !methodCall.getQualifier().isNonNullString()) {
              return methodCall;
            }
            return BinaryExpression.newBuilder()
                .setLeftOperand(methodCall.getQualifier())
                .setRightOperand(Iterables.getOnlyElement(methodCall.getArguments()))
                .setOperator(BinaryOperator.EQUALS)
                .build();
          }
        });
  }

  private static MethodDescriptor getStringEquals() {
    return TypeDescriptors.get()
        .javaLangString
        .getMethodDescriptorByName(
            MethodDescriptor.EQUALS_METHOD_NAME, TypeDescriptors.get().javaLangObject);
  }
}
