/*
 * Copyright 2026 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Synthesizes byteValue() and shortValue() overrides for classes directly extending
 * java.lang.Number if they are not already overridden.
 */
public class SynthesizeKotlinNumberOverrides extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Type rewriteType(Type type) {
            if (type.isInterface() || type.isNative()) {
              return type;
            }

            TypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
            if (superTypeDescriptor == null
                || !TypeDescriptors.isJavaLangNumber(superTypeDescriptor)) {
              return type;
            }

            synthesizeNumberOverrides(type);
            return type;
          }
        });
  }

  private void synthesizeNumberOverrides(Type type) {
    if (!type.containsMethod(
        m -> m.getName().equals("byteValue") && m.getParameterTypeDescriptors().isEmpty())) {
      type.addMember(synthesizeNumberOverride(type, "byteValue", "toByte", PrimitiveTypes.BYTE));
    }

    if (!type.containsMethod(
        m -> m.getName().equals("shortValue") && m.getParameterTypeDescriptors().isEmpty())) {
      type.addMember(synthesizeNumberOverride(type, "shortValue", "toShort", PrimitiveTypes.SHORT));
    }
  }

  private Method synthesizeNumberOverride(
      Type type, String methodName, String ktName, PrimitiveTypeDescriptor returnType) {
    MethodDescriptor intValueMethodDescriptor =
        type.getTypeDescriptor().getMethodDescriptor("intValue");

    MethodDescriptor methodDescriptor =
        MethodDescriptor.Builder.from(intValueMethodDescriptor)
            .setName(methodName)
            .setOriginalKtInfo(KtInfo.newBuilder().setName(ktName).build())
            .setReturnTypeDescriptor(returnType)
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .setAbstract(false)
            .setNative(false)
            .build();

    return Method.newBuilder()
        .setMethodDescriptor(methodDescriptor)
        .setForcedJavaOverride(true)
        .addStatements(
            ReturnStatement.newBuilder()
                .setExpression(
                    CastExpression.newBuilder()
                        .setExpression(MethodCall.Builder.from(intValueMethodDescriptor).build())
                        .setCastTypeDescriptor(returnType)
                        .build())
                .setSourcePosition(type.getSourcePosition())
                .build())
        .setSourcePosition(type.getSourcePosition())
        .build();
  }
}
