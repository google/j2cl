/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.MethodDescriptor;

/**
 * Convert lambda expressions to implementor classes for interfaces which are not functional in
 * Kotlin.
 */
public class NormalizeLambdaExpressionsJ2kt
    extends ImplementLambdaExpressionsViaImplementorClasses {
  @Override
  protected boolean shouldRewrite(FunctionExpression functionExpression) {
    MethodDescriptor methodDescriptor =
        functionExpression
            .getTypeDescriptor()
            .getFunctionalInterface()
            .getTypeDeclaration()
            .toUnparameterizedTypeDescriptor()
            .getSingleAbstractMethodDescriptor();
    boolean isKtFunctionalInterface =
        !methodDescriptor.isKtProperty()
            && methodDescriptor.getTypeParameterTypeDescriptors().isEmpty();
    return !isKtFunctionalInterface;
  }
}
