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
import static com.google.j2cl.transpiler.ast.AstUtils.isWasmJsExportedType;

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
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterDescriptors());

    ImmutableList<Expression> arguments =
        Streams.zip(
                parameters.stream(),
                methodDescriptor.getParameterTypeDescriptors().stream(),
                (parameter, typeDescriptor) ->
                    convertToInternal(
                        parameter.createReference(), typeDescriptor, origin.isWasmJsExport()))
            .collect(toImmutableList());

    TypeDescriptor returnType = methodDescriptor.getReturnTypeDescriptor();

    return Method.builder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(
            createBridgeTargetInvocation(
                methodDescriptor, sourcePosition, arguments, returnType, origin))
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
                        bridgeMethodDescriptor.getOrigin().isWasmJsExport()))
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
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterDescriptors()).get(0);

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
                        bridgeMethodDescriptor.getOrigin().isWasmJsExport()))
                .makeStatement(sourcePosition))
        .setSourcePosition(sourcePosition)
        .build();
  }

  private static Statement createBridgeTargetInvocation(
      MethodDescriptor methodDescriptor,
      SourcePosition sourcePosition,
      List<Expression> arguments,
      TypeDescriptor returnTypeDescriptor,
      MethodDescriptor.MethodOrigin origin) {
    if (methodDescriptor.isConstructor()) {
      checkState(
          returnTypeDescriptor.isSameBaseType(methodDescriptor.getEnclosingTypeDescriptor()));
      return ReturnStatement.builder()
          .setExpression(
              NewInstance.builderFrom(methodDescriptor.getDeclarationDescriptor())
                  .setArguments(AstUtils.maybePackageVarargs(methodDescriptor, arguments))
                  .build())
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
                  returnStatement.getExpression(), returnTypeDescriptor, origin.isWasmJsExport()))
          .build();
    }
    return forwardingStatement;
  }

  private static MethodDescriptor createBridgeDescriptor(
      MethodDescriptor descriptor, MethodDescriptor.MethodOrigin origin) {
    MethodDescriptor.Builder builder =
        descriptor.toBuilder()
            .makeBridge(
                origin, /* originDescriptor= */ descriptor, /* targetDescriptor= */ descriptor)
            .setReturnTypeDescriptor(
                descriptor.isConstructor()
                    ? descriptor.getReturnTypeDescriptor()
                    : getExternalType(
                        descriptor.getReturnTypeDescriptor(), origin.isWasmJsExport()))
            .setParameterDescriptors(
                descriptor.getParameterDescriptors().stream()
                    .map(
                        pd ->
                            pd.toBuilder()
                                .setTypeDescriptor(
                                    getExternalType(
                                        pd.getTypeDescriptor(), origin.isWasmJsExport()))
                                .setVarargs(false)
                                .build())
                    .collect(toImmutableList()));
    if (descriptor.isConstructor()) {
      // Change constructors to static factory methods.
      builder.setStatic(true).setConstructor(false);
    }
    return builder.build();
  }

  private static MethodDescriptor createGetterBridgeDescriptor(FieldDescriptor fieldDescriptor) {
    var origin = MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_GETTER_EXPORT;
    return AstUtils.getGetterMethodDescriptor(fieldDescriptor).toBuilder()
        .setOrigin(origin)
        .setReturnTypeDescriptor(
            getExternalType(fieldDescriptor.getTypeDescriptor(), origin.isWasmJsExport()))
        .build();
  }

  private static MethodDescriptor createSetterBridgeDescriptor(FieldDescriptor fieldDescriptor) {
    var origin = MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_SETTER_EXPORT;
    return AstUtils.getSetterMethodDescriptor(fieldDescriptor).toBuilder()
        .setOrigin(origin)
        .setParameterTypeDescriptors(
            getExternalType(fieldDescriptor.getTypeDescriptor(), origin.isWasmJsExport()))
        .build();
  }

  /** Returns the corresponding JS type for the given Wasm Java type. */
  public static TypeDescriptor getExternalType(TypeDescriptor typeDescriptor, boolean isExport) {
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(typeDescriptor.isNullable());
    }

    if (TypeDescriptors.isBoxedBooleanOrDoubleOrLong(typeDescriptor)
        || TypeDescriptors.isJavaLangObject(typeDescriptor)
        || typeDescriptor.isJsFunctionInterface()
        || needsBoundaryExternConversion(typeDescriptor, isExport)) {
      // Use externref since it can either be:
      //   - a Js primitive valus that can also be null,
      //   - an (opaque) wasm object that will cross the boundary
      //   - a type that is explicitly allowed to cross the boundary (e.g a JsType)
      return TypeDescriptors.get()
          .javaemulInternalWasmExtern
          .toNullable(typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }

  /** Converts the given expression to a JS type which can be passed to JS. */
  public static Expression convertToExternal(
      Expression expression, TypeDescriptor typeDescriptor, boolean isExport) {
    if (TypeDescriptors.isBoxedBooleanOrDoubleOrLong(typeDescriptor)
        || TypeDescriptors.isJavaLangString(typeDescriptor)
        || TypeDescriptors.isJavaLangObject(typeDescriptor)) {
      return RuntimeMethods.createToJsMethodCall(
          (DeclaredTypeDescriptor) typeDescriptor,
          // TODO(b/545779164): The cast here shouldn't be needed, but the export bridge creator
          // might create the export bridge on specializing/default bridge. In any case when
          // the cast is not really needed it gets optimized away.
          CastExpression.builder()
              .setExpression(expression)
              .setCastTypeDescriptor(typeDescriptor)
              .build());
    }
    if (typeDescriptor.isJsFunctionInterface()) {
      MethodDescriptor toJsMethodDescriptor =
          TypeDescriptors.get()
              .javaemulInternalJsFunctionAdaptor
              .getMethodDescriptor("toJs", TypeDescriptors.get().javaemulInternalJsFunctionAdaptor);
      return MethodCall.builderFrom(toJsMethodDescriptor)
          .setArguments(
              CastExpression.builder()
                  .setExpression(expression)
                  .setCastTypeDescriptor(TypeDescriptors.get().javaemulInternalJsFunctionAdaptor)
                  .build())
          .build();
    }
    if (needsBoundaryExternConversion(typeDescriptor, isExport)) {
      return RuntimeMethods.createWasmConvertToExternMethodCall(expression);
    }
    return expression;
  }

  /** Converts the given expression that was received from JS to a Wasm Java type. */
  public static Expression convertToInternal(
      Expression expression, TypeDescriptor typeDescriptor, boolean isExport) {
    if (TypeDescriptors.isBoxedBooleanOrDoubleOrLong(typeDescriptor)
        || TypeDescriptors.isJavaLangString(typeDescriptor)
        || TypeDescriptors.isJavaLangObject(typeDescriptor)) {
      return RuntimeMethods.createFromJsMethodCall(
          (DeclaredTypeDescriptor) typeDescriptor, expression);
    }
    if (typeDescriptor.isJsFunctionInterface()) {
      return RuntimeMethods.createFromJsMethodCall(
          TypeDescriptors.get().javaemulInternalJsFunctionAdaptor, expression);
    }
    if (needsBoundaryExternConversion(typeDescriptor, isExport)) {
      return RuntimeMethods.createWasmConvertToAnyMethodCall(expression, typeDescriptor);
    }
    return expression;
  }

  private static boolean needsBoundaryExternConversion(
      TypeDescriptor typeDescriptor, boolean isExport) {
    return !typeDescriptor.isNative()
        && !typeDescriptor.isPrimitive()
        && !TypeDescriptors.isWasmFuncref(typeDescriptor)
        // For methods exported by configureAll, we can avoid conversions for exported types.
        && !(isExport && isWasmJsExportedType(typeDescriptor));
  }

  private WasmExportBridgesUtils() {}
}
