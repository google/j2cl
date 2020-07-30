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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Streams;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Checks circumstances where a bridge method should be generated and creates the bridge methods.
 */
public class BridgeMethodsCreator extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (type.isNative() || type.isInterface()) {
      return;
    }
    // Create bridges
    type.getTypeDescriptor().getPolymorphicMethods().stream()
        .filter(MethodDescriptor::isBridge)
        .filter(m -> m.isMemberOf(type.getDeclaration()))
        .forEach(m -> type.addMethod(createBridgeMethod(type, m)));
  }

  /** Returns bridge method that calls the targeted method in its body. */
  private static Method createBridgeMethod(Type type, MethodDescriptor bridgeMethodDescriptor) {
    MethodDescriptor causeMethod = bridgeMethodDescriptor.getBridgeOrigin();
    MethodDescriptor targetMethod =
        adjustTargetForJsFunction(bridgeMethodDescriptor, bridgeMethodDescriptor.getBridgeTarget());

    List<Variable> parameters =
        bridgeMethodDescriptor.isGeneralizingdBridge()
            ? createBridgeParameters(bridgeMethodDescriptor)
            : AstUtils.createParameterVariables(
                bridgeMethodDescriptor.getParameterTypeDescriptors());

    List<Expression> arguments =
        Streams.zip(
                parameters.stream(),
                targetMethod.getParameterTypeDescriptors().stream(),
                BridgeMethodsCreator::performRuntimeChecksOnParameter)
            .collect(Collectors.toList());

    DeclaredTypeDescriptor targetEnclosingTypeDescriptor =
        targetMethod.getEnclosingTypeDescriptor();
    Expression qualifier =
        bridgeMethodDescriptor.isSpecializingBridge()
            ? new SuperReference(targetEnclosingTypeDescriptor)
            : new ThisReference(targetEnclosingTypeDescriptor);

    checkArgument(bridgeMethodDescriptor.isSynthetic());
    checkArgument(bridgeMethodDescriptor.isBridge());

    // TODO(b/31312257): fix or decide to not emit @override and suppress the error.
    // Determine whether the method needs @override annotation.
    boolean isOverride =
        bridgeMethodDescriptor.isGeneralizingdBridge()
            && AstUtils.overrideNeedsAtOverrideAnnotation(causeMethod);
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
        .setOverride(isOverride)
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
    Expression parameterReference = parameter.getReference();

    // If the parameter type in bridge method is different from the one in the method
    // that will handle the call, add a cast to perform the runtime type check.
    return parameter.getTypeDescriptor().isAssignableTo(requiredType)
        ? parameterReference
        : CastExpression.newBuilder()
            .setExpression(parameterReference)
            .setCastTypeDescriptor(requiredType)
            .build();
  }

  private static List<Variable> createBridgeParameters(MethodDescriptor bridgeMethodDescriptor) {
    List<Variable> parameters = new ArrayList<>();
    for (int i = 0; i < bridgeMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      parameters.add(
          Variable.newBuilder()
              .setName("arg" + i)
              // Set the type for the parameter variables in the AST from the declaration
              // of the causing methods (using its raw type to potentially remove type variables
              // that are not in context here). This is done to support the runtime type check
              // cast, because in the cases where the method is called from a supertype, the actual
              // objects passed are only guaranteed to be of the type declared by the method in the
              // super class.
              // And If we were to declare the parameter with the more specific type, the runtime
              // type check cast that is inserted below would be considered redundant are removed at
              // a later stage.
              .setTypeDescriptor(
                  bridgeMethodDescriptor
                      .getBridgeOrigin()
                      .getDeclarationDescriptor()
                      .getParameterTypeDescriptors()
                      .get(i)
                      .toRawTypeDescriptor())
              .setParameter(true)
              .build());
    }
    return parameters;
  }

  private static MethodDescriptor adjustTargetForJsFunction(
      MethodDescriptor causeMethod, MethodDescriptor targetMethod) {
    // The MethodDescriptor of the targeted method.
    MethodDescriptor.Builder methodDescriptorBuilder = MethodDescriptor.Builder.from(targetMethod);

    // If a JsFunction method needs a bridge, only the bridge method is a JsFunction method, and it
    // targets to *real* implementation, which is not a JsFunction method.
    // If both a method and the bridge method are JsMethod, only the bridge method is a JsMethod,
    // and it targets the *real* implementation, which should be emit as non-JsMethod.
    if (causeMethod.isJsMethod() && targetMethod.inSameTypeAs(causeMethod)) {
      methodDescriptorBuilder.removeParameterOptionality().setJsInfo(JsInfo.NONE);
    }

    return methodDescriptorBuilder.setJsFunction(false).build();
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
