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
package com.google.j2cl.transpiler.ast;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.common.SourcePosition;
import java.util.List;

/** Utility for generating bridge methods for Wasm entry points and exported methods. */
public class WasmExportBridgesUtils {

  /**
   * Generates a bridge method, intended to be exported, that defers to the specified method and
   * does any necessary JS <-> Wasm argument and return value conversions.
   */
  public static Method generateBridge(
      MethodDescriptor methodDescriptor,
      SourcePosition sourcePosition,
      MethodDescriptor.MethodOrigin origin) {
    MethodDescriptor bridgeMethodDescriptor = createBridgeDescriptor(methodDescriptor, origin);
    List<Variable> parameters =
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterTypeDescriptors());

    ImmutableList<Expression> arguments =
        Streams.zip(
                parameters.stream(),
                methodDescriptor.getParameterTypeDescriptors().stream(),
                WasmExportBridgesUtils::convertArgumentIfNeeded)
            .collect(toImmutableList());

    TypeDescriptor returnType = methodDescriptor.getReturnTypeDescriptor();

    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(
            convertReturnIfNeeded(
                AstUtils.createForwardingStatement(
                    sourcePosition,
                    /* qualifier= */ methodDescriptor.isStatic()
                        ? null
                        : new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()),
                    methodDescriptor,
                    /* isStaticDispatch= */ methodDescriptor.isStatic(),
                    arguments,
                    returnType),
                returnType))
        .setJsDocDescription(
            origin.isWasmEntryPoint() ? "Wasm entry point forwarding method." : null)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /** Creates the argument expression, containing any necessary conversions. */
  private static Expression convertArgumentIfNeeded(
      Variable parameter, TypeDescriptor targetArgumentDescriptor) {
    Expression reference = parameter.createReference();
    if (TypeDescriptors.isJavaLangString(targetArgumentDescriptor)) {
      return RuntimeMethods.createStringFromJsStringMethodCall(reference);
    }
    return reference;
  }

  private static Statement convertReturnIfNeeded(
      Statement statement, TypeDescriptor targetReturnTypeDescriptor) {
    if (TypeDescriptors.isJavaLangString(targetReturnTypeDescriptor)) {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      return ReturnStatement.Builder.from(returnStatement)
          .setExpression(
              RuntimeMethods.createJsStringFromStringMethodCall(returnStatement.getExpression()))
          .build();
    }
    return statement;
  }

  private static MethodDescriptor createBridgeDescriptor(
      MethodDescriptor descriptor, MethodDescriptor.MethodOrigin origin) {
    MethodDescriptor.Builder builder =
        MethodDescriptor.Builder.from(descriptor)
            .setOrigin(origin)
            .setReturnTypeDescriptor(
                replaceStringWithNativeString(descriptor.getReturnTypeDescriptor()))
            .updateParameterTypeDescriptors(
                descriptor.getParameterTypeDescriptors().stream()
                    .map(WasmExportBridgesUtils::replaceStringWithNativeString)
                    .collect(toImmutableList()));
    if (!origin.isWasmEntryPoint()) {
      builder.setOriginalJsInfo(
          descriptor.getJsInfo().toBuilder().setJsName(descriptor.getSimpleJsName()).build());
    }
    return builder.build();
  }

  private static TypeDescriptor replaceStringWithNativeString(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }

  private WasmExportBridgesUtils() {}
}
