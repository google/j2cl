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
                (parameter, typeDescriptor) ->
                    convertToInternal(
                        parameter.createReference(), typeDescriptor, /* isExport= */ true))
            .collect(toImmutableList());

    TypeDescriptor returnType = methodDescriptor.getReturnTypeDescriptor();

    return Method.builder()
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

    return Method.builder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .addStatements(
            ReturnStatement.builder()
                .setExpression(
                    convertToExternal(
                        FieldAccess.builderFrom(fieldDescriptor)
                            .setDefaultInstanceQualifier()
                            .build(),
                        fieldDescriptor.getTypeDescriptor(),
                        /* isExport= */ true))
                .setSourcePosition(sourcePosition)
                .build())
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

    return Method.builder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(valueParameter)
        .addStatements(
            FieldAccess.builderFrom(fieldDescriptor)
                .setDefaultInstanceQualifier()
                .build()
                .infixAssign(
                    convertToInternal(
                        valueParameter.createReference(),
                        fieldDescriptor.getTypeDescriptor(),
                        /* isExport= */ true))
                .makeStatement(sourcePosition))
        .setSourcePosition(sourcePosition)
        .build();
  }

  private static Statement createBridgeTargetInvocation(
      MethodDescriptor methodDescriptor,
      SourcePosition sourcePosition,
      List<Expression> arguments,
      TypeDescriptor returnTypeDescriptor) {
    if (methodDescriptor.isConstructor()) {
      checkState(
          returnTypeDescriptor.isSameBaseType(methodDescriptor.getEnclosingTypeDescriptor()));
      return ReturnStatement.builder()
          .setExpression(
              convertToExternal(
                  NewInstance.builderFrom(methodDescriptor.getDeclarationDescriptor())
                      .setArguments(AstUtils.maybePackageVarargs(methodDescriptor, arguments))
                      .build(),
                  returnTypeDescriptor,
                  /* isExport= */ true))
          .setSourcePosition(sourcePosition)
          .build();
    }

    var forwardingStatement =
        AstUtils.createForwardingStatement(
            sourcePosition,
            /* qualifier= */ methodDescriptor.isStatic()
                ? null
                : new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()),
            methodDescriptor,
            /* isStaticDispatch= */ methodDescriptor.isStatic(),
            arguments,
            returnTypeDescriptor);
    if (forwardingStatement instanceof ReturnStatement returnStatement
        && returnStatement.getExpression() != null) {
      // If a value is returned, convert it to an external type.
      return returnStatement.toBuilder()
          .setExpression(
              convertToExternal(
                  returnStatement.getExpression(), returnTypeDescriptor, /* isExport= */ true))
          .build();
    }
    return forwardingStatement;
  }

  private static MethodDescriptor createBridgeDescriptor(
      MethodDescriptor descriptor, MethodDescriptor.MethodOrigin origin) {
    MethodDescriptor.Builder builder =
        descriptor.toBuilder()
            .setOrigin(origin)
            .setReturnTypeDescriptor(
                getExternalType(descriptor.getReturnTypeDescriptor(), /* isExport= */ true))
            .updateParameterTypeDescriptors(
                descriptor.getParameterTypeDescriptors().stream()
                    .map(parameterType -> getExternalType(parameterType, /* isExport= */ true))
                    .collect(toImmutableList()))
            .makeDeclaration()
            .setAbstract(false)
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
    return AstUtils.getGetterMethodDescriptor(fieldDescriptor).toBuilder()
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_GETTER_EXPORT)
        .setReturnTypeDescriptor(
            getExternalType(fieldDescriptor.getTypeDescriptor(), /* isExport= */ true))
        .build();
  }

  private static MethodDescriptor createSetterBridgeDescriptor(FieldDescriptor fieldDescriptor) {
    return AstUtils.getSetterMethodDescriptor(fieldDescriptor).toBuilder()
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_SETTER_EXPORT)
        .setParameterTypeDescriptors(
            getExternalType(fieldDescriptor.getTypeDescriptor(), /* isExport= */ true))
        .build();
  }

  /** Returns the corresponding JS type for the given Wasm Java type. */
  public static TypeDescriptor getExternalType(
      TypeDescriptor javaTypeDescriptor, boolean isExport) {
    if (TypeDescriptors.isJavaLangString(javaTypeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(javaTypeDescriptor.isNullable());
    }
    if (javaTypeDescriptor.isNative() || javaTypeDescriptor.isPrimitive()) {
      return javaTypeDescriptor;
    }
    if (isExport) {
      return javaTypeDescriptor;
    }
    return TypeDescriptors.get()
        .javaemulInternalWasmExtern
        .toNullable(javaTypeDescriptor.isNullable());
  }

  /** Converts the given expression to a JS type which can be passed to JS. */
  public static Expression convertToExternal(
      Expression expression, TypeDescriptor javaTypeDescriptor, boolean isExport) {
    if (TypeDescriptors.isJavaLangString(javaTypeDescriptor)) {
      return RuntimeMethods.createJsStringFromStringMethodCall(expression);
    }
    if (javaTypeDescriptor.isNative() || javaTypeDescriptor.isPrimitive()) {
      return expression;
    }
    if (isExport) {
      return expression;
    }
    return RuntimeMethods.createWasmExternalizeMethodCall(expression);
  }

  /** Converts the given expression that was received from JS to a Wasm Java type. */
  public static Expression convertToInternal(
      Expression expression, TypeDescriptor javaTypeDescriptor, boolean isExport) {
    if (TypeDescriptors.isJavaLangString(javaTypeDescriptor)) {
      return RuntimeMethods.createStringFromJsStringMethodCall(expression);
    }
    if (javaTypeDescriptor.isJsFunctionInterface()) {
      MethodDescriptor adaptMethodDescriptor =
          LambdaAdaptorTypeDescriptors.getWasmJsFunctionAdaptMethod(
              LambdaAdaptorTypeDescriptors.createFunctionalInterfaceAdaptorTypeDescriptor(
                  javaTypeDescriptor));
      return MethodCall.builderFrom(adaptMethodDescriptor).setArguments(expression).build();
    }
    if (javaTypeDescriptor.isNative() || javaTypeDescriptor.isPrimitive()) {
      return expression;
    }
    if (isExport) {
      return expression;
    }
    return RuntimeMethods.createWasmInternalizeMethodCall(expression, javaTypeDescriptor);
  }

  private WasmExportBridgesUtils() {}
}
