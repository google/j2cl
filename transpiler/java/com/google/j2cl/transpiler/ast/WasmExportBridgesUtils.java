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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.AstUtils.isWasmJsExportedType;

import com.google.common.collect.Streams;
import com.google.j2cl.common.SourcePosition;
import java.util.List;
import java.util.stream.Stream;

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

    return Method.builder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(
            createBridgeTargetInvocation(
                methodDescriptor,
                sourcePosition,
                parameters.stream().map(Variable::createReference).collect(toImmutableList()),
                methodDescriptor.getReturnTypeDescriptor(),
                origin))
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

  /**
   * Creates a call to the bridge target method, with necessary JS <-> Wasm conversions of the
   * arguments and return value.
   */
  private static Statement createBridgeTargetInvocation(
      MethodDescriptor methodDescriptor,
      SourcePosition sourcePosition,
      List<Expression> arguments,
      TypeDescriptor targetReturnTypeDescriptor,
      MethodDescriptor.MethodOrigin origin) {
    List<Expression> convertedArguments =
        convertAllToInternal(
            arguments.stream(), methodDescriptor.getParameterTypeDescriptors(), origin);

    if (methodDescriptor.isConstructor()) {
      checkState(
          targetReturnTypeDescriptor.isSameBaseType(methodDescriptor.getEnclosingTypeDescriptor()));
      return ReturnStatement.builder()
          .setExpression(
              NewInstance.builderFrom(methodDescriptor.getDeclarationDescriptor())
                  .setArguments(AstUtils.maybePackageVarargs(methodDescriptor, convertedArguments))
                  .build())
          .setSourcePosition(sourcePosition)
          .build();
    }

    return createBridgeReturnStatement(
        AstUtils.createForwardingStatement(
            sourcePosition,
            /* qualifier= */ methodDescriptor.isStatic()
                ? null
                : new ThisReference(methodDescriptor.getEnclosingTypeDescriptor()),
            methodDescriptor,
            /* isStaticDispatch= */ methodDescriptor.isStatic(),
            convertedArguments,
            targetReturnTypeDescriptor),
        targetReturnTypeDescriptor,
        origin);
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

  /**
   * Generates a static export bridge for a JsFunction interface, intended to be exported via
   * JsFunctionAdaptor.
   */
  public static Method generateJsFunctionBridge(
      DeclaredTypeDescriptor jsFunctionTypeDescriptor, SourcePosition sourcePosition) {
    checkArgument(jsFunctionTypeDescriptor.isJsFunctionInterface());
    MethodDescriptor bridgeMethodDescriptor =
        getJsFunctionBridgeDescriptor(jsFunctionTypeDescriptor);
    MethodDescriptor functionalMethod =
        jsFunctionTypeDescriptor.getJsFunctionMethodDescriptor().getDeclarationDescriptor();
    List<Variable> parameters =
        AstUtils.createParameterVariables(bridgeMethodDescriptor.getParameterDescriptors());

    Variable adaptorParameter = parameters.get(0);
    List<Expression> convertedArguments =
        convertAllToInternal(
            parameters.stream().skip(1).map(Variable::createReference),
            functionalMethod.getParameterTypeDescriptors(),
            bridgeMethodDescriptor.getOrigin());

    WasmFuncrefCall wasmFuncrefCall =
        WasmFuncrefCall.builder()
            .setInstance(adaptorParameter.createReference())
            .setFunctionalInterface(jsFunctionTypeDescriptor.toRawTypeDescriptor())
            .setFuncref(
                FieldAccess.builder()
                    .setQualifier(adaptorParameter.createReference())
                    .setTarget(
                        TypeDescriptors.get()
                            .javaemulInternalJsFunctionAdaptor
                            .getFieldDescriptor("wasmFuncref"))
                    .build())
            .setArguments(convertedArguments)
            .build();

    // static R $js_export_run(JsFunctionAdaptor adaptor, T param) {
    //   return toJs(
    //     adaptor.wasmFuncref(adaptor, fromJs(param)));
    // }
    return Method.builder()
        .setMethodDescriptor(bridgeMethodDescriptor)
        .setParameters(parameters)
        .addStatements(
            createBridgeReturnStatement(
                wasmFuncrefCall,
                sourcePosition,
                functionalMethod.getReturnTypeDescriptor(),
                bridgeMethodDescriptor.getOrigin()))
        .setSourcePosition(sourcePosition)
        .build();
  }

  /** Returns the descriptor of the JsFunction export bridge method on the given interface. */
  public static MethodDescriptor getJsFunctionBridgeDescriptor(
      DeclaredTypeDescriptor jsFunctionTypeDescriptor) {
    checkArgument(jsFunctionTypeDescriptor.isJsFunctionInterface());
    MethodDescriptor functionalMethod =
        jsFunctionTypeDescriptor.getJsFunctionMethodDescriptor().getDeclarationDescriptor();
    return createBridgeDescriptor(
            functionalMethod, MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_FUNCTION_EXPORT)
        .toBuilder()
        .setEnclosingTypeDescriptor(jsFunctionTypeDescriptor.toRawTypeDescriptor())
        .setStatic(true)
        .addParameterTypeDescriptors(0, TypeDescriptors.get().javaemulInternalJsFunctionAdaptor)
        .build();
  }

  private static Statement createBridgeReturnStatement(
      Expression forwardingExpression,
      SourcePosition sourcePosition,
      TypeDescriptor targetReturnTypeDescriptor,
      MethodDescriptor.MethodOrigin origin) {
    return createBridgeReturnStatement(
        AstUtils.createReturnOrExpressionStatement(
            sourcePosition, forwardingExpression, targetReturnTypeDescriptor),
        targetReturnTypeDescriptor,
        origin);
  }

  private static Statement createBridgeReturnStatement(
      Statement forwardingStatement,
      TypeDescriptor targetReturnTypeDescriptor,
      MethodDescriptor.MethodOrigin origin) {
    if (forwardingStatement instanceof ReturnStatement returnStatement
        && returnStatement.getExpression() != null) {
      // If a value is returned, convert it to an external type.
      return returnStatement.toBuilder()
          .setExpression(
              convertToExternal(
                  returnStatement.getExpression(),
                  targetReturnTypeDescriptor,
                  origin.isWasmJsExport()))
          .build();
    }
    return forwardingStatement;
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

  /** Converts the given list of expressions that was received from JS to a Wasm Java type. */
  private static List<Expression> convertAllToInternal(
      Stream<Expression> expressions,
      List<TypeDescriptor> typeDescriptors,
      MethodDescriptor.MethodOrigin origin) {
    return Streams.zip(
            expressions,
            typeDescriptors.stream(),
            (expression, typeDescriptor) ->
                convertToInternal(expression, typeDescriptor, origin.isWasmJsExport()))
        .collect(toImmutableList());
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
