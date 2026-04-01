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

import static com.google.common.base.Preconditions.checkState;
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
            createBridgeTargetInvocation(methodDescriptor, sourcePosition, arguments, returnType))
        .setJsDocDescription(
            origin.isWasmEntryPoint() ? "Wasm entry point forwarding method." : null)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Generates a property getter bridge corresponding to a @JsProperty field, intended to be
   * exported, that returns the value of the specified field, performing any necessary JS <-> Wasm
   * conversions.
   */
  public static Method generateGetterBridge(
      FieldDescriptor fieldDescriptor, SourcePosition sourcePosition) {
    MethodDescriptor bridgeMethodDescriptor = createGetterBridgeDescriptor(fieldDescriptor);

    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .addStatements(
            convertReturnIfNeeded(
                AstUtils.createReturnOrExpressionStatement(
                    sourcePosition,
                    FieldAccess.Builder.from(fieldDescriptor).setDefaultInstanceQualifier().build(),
                    fieldDescriptor.getTypeDescriptor()),
                fieldDescriptor.getTypeDescriptor()))
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Generates a property setter bridge corresponding to a @JsProperty field, intended to be
   * exported, that sets the value of the specified field, performing any necessary JS <-> Wasm
   * conversions.
   */
  public static Method generateSetterBridge(
      FieldDescriptor fieldDescriptor, SourcePosition sourcePosition) {
    MethodDescriptor bridgeMethodDescriptor = createSetterBridgeDescriptor(fieldDescriptor);
    Variable valueParameter =
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterTypeDescriptors())
            .get(0);

    return Method.newBuilder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(valueParameter)
        .addStatements(
            BinaryExpression.Builder.asAssignmentTo(fieldDescriptor)
                .setRightOperand(
                    convertArgumentIfNeeded(valueParameter, fieldDescriptor.getTypeDescriptor()))
                .build()
                .makeStatement(sourcePosition))
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

  private static Statement createBridgeTargetInvocation(
      MethodDescriptor methodDescriptor,
      SourcePosition sourcePosition,
      List<Expression> arguments,
      TypeDescriptor returnTypeDescriptor) {
    if (methodDescriptor.isConstructor()) {
      checkState(
          returnTypeDescriptor.isSameBaseType(methodDescriptor.getEnclosingTypeDescriptor()));
      return ReturnStatement.newBuilder()
          .setExpression(
              NewInstance.Builder.from(methodDescriptor)
                  .setArguments(AstUtils.maybePackageVarargs(methodDescriptor, arguments))
                  .build())
          .setSourcePosition(sourcePosition)
          .build();
    }

    return convertReturnIfNeeded(
        AstUtils.createForwardingStatement(
            sourcePosition,
            /* qualifier= */ methodDescriptor.isStatic()
                ? null
                : new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()),
            methodDescriptor,
            /* isStaticDispatch= */ methodDescriptor.isStatic(),
            arguments,
            returnTypeDescriptor),
        returnTypeDescriptor);
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
                    .collect(toImmutableList()))
            // Copy over the JsInfo from the descriptor. This allows the bridge to retain the JsInfo
            // if, for example, it is inherited; we otherwise lose the inherited JsInfo because we
            // lose override information. It also preserves the CONSTRUCTOR member type if the
            // original member is a constructor.
            // TODO(b/493656775): Consider calling `makeBridge` here instead and getting JsInfo from
            // the mangling descriptor/bridge origin.
            .setOriginalJsInfo(descriptor.getJsInfo());
    if (descriptor.isConstructor()) {
      // Change constructors to static factory methods.
      builder.setStatic(true).setConstructor(false);
    }
    return builder.build();
  }

  private static MethodDescriptor createGetterBridgeDescriptor(FieldDescriptor fieldDescriptor) {
    return buildPropertyBridgeDescriptor(fieldDescriptor, JsMemberType.GETTER)
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_GETTER_EXPORT)
        .setReturnTypeDescriptor(replaceStringWithNativeString(fieldDescriptor.getTypeDescriptor()))
        .build();
  }

  private static MethodDescriptor createSetterBridgeDescriptor(FieldDescriptor fieldDescriptor) {
    return buildPropertyBridgeDescriptor(fieldDescriptor, JsMemberType.SETTER)
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_SETTER_EXPORT)
        .setParameterTypeDescriptors(
            replaceStringWithNativeString(fieldDescriptor.getTypeDescriptor()))
        .build();
  }

  private static MethodDescriptor.Builder buildPropertyBridgeDescriptor(
      FieldDescriptor fieldDescriptor, JsMemberType jsMemberType) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(fieldDescriptor.getEnclosingTypeDescriptor())
        .setName(fieldDescriptor.getName())
        .setVisibility(fieldDescriptor.getVisibility())
        .setStatic(fieldDescriptor.isStatic())
        .setOriginalJsInfo(
            fieldDescriptor.getJsInfo().toBuilder()
                .setJsMemberType(jsMemberType)
                // Name has to be set here to avoid JsMemberType.GETTER/SETTER.computeJsName()
                // returning null.
                // TODO(b/493656775): Revisit this when creating these bridges using `makeBridge`.
                // Perhaps the bridge origin should be a MemberDescriptor.
                .setJsName(fieldDescriptor.getSimpleJsName())
                .build());
  }

  private static TypeDescriptor replaceStringWithNativeString(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }

  private WasmExportBridgesUtils() {}
}
