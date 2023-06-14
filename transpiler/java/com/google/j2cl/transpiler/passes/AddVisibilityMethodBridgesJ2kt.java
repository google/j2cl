/*
 * Copyright 2022 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.List;

/** Add bridge for public or protected methods which override package-private ones. */
public class AddVisibilityMethodBridgesJ2kt extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (!type.isClass() || type.isNative()) {
      return;
    }

    for (Method method : type.getMethods()) {
      MethodDescriptor methodDescriptor = method.getDescriptor();
      if (AstUtils.needsVisibilityBridge(methodDescriptor)) {
        type.addMember(createBridgeMethod(type, methodDescriptor));
      }
    }
  }

  private static Method createBridgeMethod(Type type, MethodDescriptor targetMethod) {
    List<Variable> parameters =
        AstUtils.createParameterVariables(targetMethod.getParameterTypeDescriptors());

    ImmutableList<Expression> arguments =
        parameters.stream().map(Variable::createReference).collect(toImmutableList());

    return Method.newBuilder()
        .setMethodDescriptor(
            MethodDescriptor.Builder.from(targetMethod)
                .setDeclarationDescriptor(null)
                .setEnclosingTypeDescriptor(type.getTypeDescriptor())
                .setVisibility(Visibility.PACKAGE_PRIVATE)
                .setNative(false)
                .setAbstract(false)
                .build())
        .setParameters(parameters)
        .addStatements(
            AstUtils.createForwardingStatement(
                type.getSourcePosition(),
                new ThisReference(type.getTypeDescriptor()),
                targetMethod,
                false,
                arguments,
                targetMethod.getReturnTypeDescriptor()))
        .setSourcePosition(type.getSourcePosition())
        .build();
  }
}
