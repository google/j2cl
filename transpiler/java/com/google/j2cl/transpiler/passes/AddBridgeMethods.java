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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.List;

/**
 * Checks circumstances where a bridge method should be generated and creates the bridge methods.
 */
public class AddBridgeMethods extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (type.isNative() || type.isInterface()) {
      return;
    }
    // Create bridges
    type.getTypeDescriptor().getPolymorphicMethods().stream()
        .filter(MethodDescriptor::isBridge)
        .filter(m -> m.isMemberOf(type.getDeclaration()))
        .forEach(m -> type.addMember(createBridgeMethod(type, m)));
  }

  /** Returns bridge method that calls the targeted method in its body. */
  private static Method createBridgeMethod(Type type, MethodDescriptor bridgeMethodDescriptor) {
    MethodDescriptor targetMethod = bridgeMethodDescriptor.getBridgeTarget();

    List<Variable> parameters =
        AstUtils.createParameterVariables(
            bridgeMethodDescriptor.getDispatchParameterTypeDescriptors());

    List<Expression> arguments =
        Streams.zip(
                parameters.stream(),
                targetMethod.getParameterTypeDescriptors().stream(),
                AddBridgeMethods::performRuntimeChecksOnParameter)
            .collect(toImmutableList());

    DeclaredTypeDescriptor targetEnclosingTypeDescriptor =
        targetMethod.getEnclosingTypeDescriptor();
    Expression qualifier =
        bridgeMethodDescriptor.isSpecializingBridge()
            ? new SuperReference(targetEnclosingTypeDescriptor)
            : new ThisReference(type.getTypeDescriptor());

    checkArgument(bridgeMethodDescriptor.isSynthetic());
    checkArgument(bridgeMethodDescriptor.isBridge());

    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(
            AstUtils.createForwardingStatement(
                type.getSourcePosition(),
                qualifier,
                targetMethod,
                targetMethod.isDefaultMethod(),
                arguments,
                bridgeMethodDescriptor.getReturnTypeDescriptor()))
        .setJsDocDescription(getJsDocDescription(bridgeMethodDescriptor))
        .setSourcePosition(type.getSourcePosition())
        .build();
  }

  /**
   * Returns an expression of the type required in the forwarding call, inserting a cast if
   * necessary.
   */
  private static Expression performRuntimeChecksOnParameter(
      Variable parameter, TypeDescriptor requiredType) {
    Expression parameterReference = parameter.createReference();

    // If the parameter type in bridge method is different from the one in the method
    // that will handle the call, add a cast to perform the runtime type check.
    return parameter.getTypeDescriptor().isAssignableTo(requiredType)
        ? parameterReference
        : CastExpression.newBuilder()
            .setExpression(parameterReference)
            .setCastTypeDescriptor(requiredType)
            .build();
  }

  private static String getJsDocDescription(MethodDescriptor bridgeMethodDescriptor) {
    switch (bridgeMethodDescriptor.getOrigin()) {
      case GENERALIZING_BRIDGE:
        return "Bridge method.";
      case SPECIALIZING_BRIDGE:
        return "Specialized bridge method.";
      case DEFAULT_METHOD_BRIDGE:
        return "Default method forwarding stub.";
      default:
        throw new IllegalArgumentException();
    }
  }
}
