/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_CONSTRUCTOR_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_METHOD_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PROPERTY_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.kotlin.ir.copyAnnotationsWhen
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.defaultArgumentsDispatchFunction
import org.jetbrains.kotlin.backend.common.defaultArgumentsOriginalFunction
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentFunctionFactory
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getJvmVisibilityOfDefaultArgumentStub
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/** Generates bridge functions to handle default parameter resolution. */
internal class J2clDefaultArgumentStubGenerator(context: J2clBackendContext) :
  DefaultArgumentStubGenerator<J2clBackendContext>(
    context = context,
    factory = J2clDefaultArgumentFunctionFactory(context),
    skipInlineMethods = false,
    skipExternalMethods = true,
  ) {

  private val localFunctionTransformer =
    object : IrVisitorVoid() {
      override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
      }

      // Don't traverse into nested classes as those will be covered by the DeclarationTransformer
      // traversal.
      override fun visitClass(declaration: IrClass) {}

      override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        if (declaration.isLocal && declaration.hasDefaultParameters) {
          declaration.introduceDefaultResolution()
        }
      }
    }

  override fun IrFunction.resolveAnnotations(): List<IrConstructorCall> = copyAnnotationsWhen {
    shouldCopyAnnotationToBridge()
  }

  override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
    if (declaration !is IrFunction || declaration.isExternalOrInheritedFromExternal()) {
      return null
    }

    // Traverse into local functions transforming them as well.
    declaration.acceptChildrenVoid(localFunctionTransformer)

    return super.transformFlat(declaration)
  }

  /**
   * Generates an expression to check if a parameter is unset and resolve it to the default, if
   * applicable.
   *
   * For example:
   * ```kotlin
   * fun foo(str: String = "defaulted") {}
   * ```
   *
   * the generated expression for `str` will be:
   * ```kotlin
   * if (isUndefined(str)) {
   *   str = "defaulted"
   * }
   * ```
   */
  private fun IrBuilderWithScope.createDefaultResolutionExpression(
    parameter: IrValueParameter,
    defaultExpression: IrExpression,
  ): IrExpression? {
    // If the parameter does not have a default initializer, or the it's just being defaulted to
    // null, then we don't need to resolve anything.
    if (defaultExpression.isNullConst()) {
      return null
    }

    return irIfThenElse(
      type = parameter.type,
      condition =
        irCall(this@J2clDefaultArgumentStubGenerator.context.intrinsics.jsIsUndefinedFunctionSymbol)
          .apply { putValueArgument(0, irGet(parameter)) },
      thenPart = defaultExpression,
      elsePart = irGet(parameter),
    )
  }

  private fun <T : IrElement> IrStatementsBuilder<T>.createDefaultResolutionToTmpVariable(
    parameter: IrValueParameter,
    defaultExpression: IrExpression,
  ): IrVariable? {
    val resolutionExpression =
      createDefaultResolutionExpression(parameter, defaultExpression) ?: return null
    return createTmpVariable(resolutionExpression, nameHint = parameter.defaultedTmpVariableName)
  }

  /**
   * Generates the body of a bridge function to handle default parameter resolution.
   *
   * For example, if the original function definition was:
   * ```kotlin
   * fun foo(str: String = "defaulted") {
   *   ...
   * }
   * ```
   *
   * the bridge method will be:
   * ```kotlin
   * fun foo(str: String = "defaulted") {
   *   ...
   * }
   *
   * fun foo$default(str: String = "defaulted") {
   *   if (isUndefined(str)) {
   *     str = "defaulted"
   *   }
   *   return foo(str)
   * }
   * ```
   *
   * Later passes will cleanup the default expression from the parameter definition.
   */
  override fun IrFunction.generateDefaultStubBody(originalDeclaration: IrFunction): IrBody {
    val newFunction = this

    val variables: MutableMap<IrValueParameter, IrValueDeclaration> =
      originalDeclaration.parameters
        .associateBy({ it }, { parameters[it.indexInParameters] })
        .toMutableMap()
    val parameterRemapper = VariableRemapper(variables)

    return context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET).irBlockBody {
      // For each optional parameter, add a resolution statement to default it if it's undefined.
      for (originalParameter in originalDeclaration.parameters.filter { it.defaultValue != null }) {
        val newParameter = parameters[originalParameter.indexInParameters]
        createDefaultResolutionToTmpVariable(
            newParameter,
            originalParameter.remapDefaultExpressionReferences(parameterRemapper),
          )
          ?.let { variables[originalParameter] = it }
      }

      // If it's a constructor we need a constructing delegating call, otherwise just delegate to
      // the original function via a normal call
      if (originalDeclaration is IrConstructor) {
        +irDelegatingConstructorCall(originalDeclaration).apply {
          passTypeArgumentsFrom(newFunction.parentAsClass)
          arguments.assignFrom(originalDeclaration.parameters) { irGet(variables[it]!!) }
        }
      } else {
        val wrappedFunctionCall =
          irCall(originalDeclaration, origin = getOriginForCallToImplementation()).apply {
            passTypeArgumentsFrom(newFunction)
            dispatchReceiver = newFunction.dispatchReceiverParameter?.let { irGet(it) }
            extensionReceiver = newFunction.extensionReceiverParameter?.let { irGet(it) }
            arguments.assignFrom(originalDeclaration.parameters) { irGet(variables[it]!!) }
          }
        +irReturn(wrappedFunctionCall)
      }
    }
  }

  /**
   * Moves default parameter initialization into the body of the function itself.
   *
   * This is intended to be used when a function can handle its own defaults without needing a
   * bridge function.
   */
  private fun IrFunction.introduceDefaultResolution() {
    // Ensure that it doesn't already have a bridge or is itself a bridge.
    check(defaultArgumentsDispatchFunction == null && defaultArgumentsOriginalFunction == null)

    val originalBlockBody = body as? IrBlockBody
    if (originalBlockBody != null) {
      val irBuilder = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
      val newVariables: MutableMap<IrValueParameter, IrValueDeclaration> =
        parameters.associateWith { it }.toMutableMap()
      val variableRemapper = VariableRemapper(newVariables)
      body =
        irBuilder.irBlockBody {
          for (parameter in parameters.filter { it.defaultValue != null }) {
            createDefaultResolutionToTmpVariable(
                parameter,
                parameter.remapDefaultExpressionReferences(variableRemapper),
              )
              ?.let { newVariables[parameter] = it }
          }

          // Remap variables from the original body and add the statements to the new body.
          originalBlockBody.transformChildrenVoid(variableRemapper)
          +originalBlockBody.statements
        }
    }

    // Mark the function as its own dispatch function.
    defaultArgumentsDispatchFunction = this
  }

  companion object {
    /** List of annotations that should not be copied to bridge functions. */
    private val annotationsToNotCopy by lazy {
      listOf(
        FqName(JS_METHOD_ANNOTATION_NAME),
        FqName(JS_CONSTRUCTOR_ANNOTATION_NAME),
        FqName(JS_PROPERTY_ANNOTATION_NAME),
      )
    }

    private fun IrConstructorCall.shouldCopyAnnotationToBridge(): Boolean =
      annotationsToNotCopy.none { isAnnotation(it) }

    private fun IrValueParameter.remapDefaultExpressionReferences(
      variableRemapper: VariableRemapper
    ): IrExpression = defaultValue!!.expression.transform(variableRemapper, data = null)

    private val IrValueParameter.defaultedTmpVariableName: String
      get() = "${name.asString()}\$defaulted"
  }
}

/** Generates the bridge function API for default resolution. */
private class J2clDefaultArgumentFunctionFactory(context: J2clBackendContext) :
  DefaultArgumentFunctionFactory(context) {

  // We'll treat null and undefined as distinct types. That is, a type can be optional without also
  // being nullable.
  override fun IrType.hasNullAsUndefinedValue(): Boolean = false

  override fun IrFunction.generateDefaultArgumentStubFrom(
    original: IrFunction,
    useConstructorMarker: Boolean,
  ) {
    copyAttributes(original)
    @Suppress("CheckReturnValue") copyTypeParametersFrom(original)
    copyReturnTypeFrom(original)
    copyReceiversFrom(original)
    copyValueParametersFrom(original)

    // Remove any varargs from the bridge function. We'll expect an array literal to be passed
    // instead. This will also allow us to support varargs with a default as well.
    for (valueParameter in valueParameters) {
      if (valueParameter.isVararg) {
        valueParameter.varargElementType = null
      }
    }

    // Kotlin allows for non-trailing default parameters, but Closure does not allow this. Instead
    // we'll make all the intermediate parameters default as well. Kotlin won't attempt to omit them
    // and Java callers are forced to pass them. We'll leave a stub to serve as a marker that the
    // parameter is optional, including for those that already had a default initializer.
    valueParameters
      .asSequence()
      .dropWhile { it.defaultValue == null }
      .forEach { it.stubDefaultValue() }

    if (useConstructorMarker) {
      val markerType = context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable()
      addValueParameter(
          "marker".synthesizedString,
          markerType,
          IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER,
        )
        .also { it.stubDefaultValue() }
    }
  }

  private fun IrValueParameter.stubDefaultValue() = stubDefaultValue(context)
}

/**
 * Cleans up default default initializers in bridge and original functions.
 *
 * In original functions the default initializers will be removed entirely -- they will no longer be
 * optional. All callers should have already been rewritten to go through the bridge instead.
 *
 * In bridge functions the default initializers will be replaced with a stub initializer which is
 * just an error expression. This is done to leave them as a marker that they are optional, but
 * avoid anyone later on relying upon the contents of the initializer as they have already been
 * extracted to statements within the body of the function.
 */
internal class J2clDefaultParameterCleaner(private val context: J2clBackendContext) :
  FileLoweringPass, IrElementVisitorVoid {
  override fun lower(irFile: IrFile) = visitElement(irFile)

  override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

  override fun visitFunction(declaration: IrFunction) {
    super.visitFunction(declaration)

    // If this function has a bridge, then we should remove the default initializers on all
    // parameters. The original function will require all parameters to always be passed. Callers
    // that want to omit parameters need to call through the bridge.
    if (declaration.hasDefaultBridge) {
      for (param in declaration.parameters) {
        param.defaultValue = null
      }
    }
  }
}

/**
 * Rewrites call sites that use default parameters to call through bridge functions.
 *
 * Any omitted arguments will be substituted with an explicit undefined reference. Arguments that
 * are explicitly passed to optional parameters will be coerced from undefined to null.
 */
internal class J2clDefaultParameterInjector(context: J2clBackendContext) :
  DefaultParameterInjector<J2clBackendContext>(
    context = context,
    factory = J2clDefaultArgumentFunctionFactory(context),
    skipInline = false,
    skipExternalMethods = false,
  ) {

  override fun nullConst(
    startOffset: Int,
    endOffset: Int,
    irParameter: IrValueParameter,
  ): IrExpression? = nullConst(startOffset, endOffset, irParameter.type)

  override fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression =
    IrCallImpl.fromSymbolOwner(
        startOffset,
        endOffset,
        this@J2clDefaultParameterInjector.context.intrinsics.jsUndefinedSymbol,
      )
      .apply {
        this@apply.type = type
        origin = IrStatementOrigin.DEFAULT_VALUE
      }

  override fun defaultArgumentStubVisibility(function: IrFunction) =
    function.getJvmVisibilityOfDefaultArgumentStub()

  override fun IrBlockBuilder.argumentsForCall(
    expression: IrFunctionAccessExpression,
    stubFunction: IrFunction,
  ): Map<IrValueParameter, IrExpression?> {
    val startOffset = expression.startOffset
    val endOffset = expression.endOffset
    val declaration = expression.symbol.owner

    // Copy over the original arguments, substituting any missing arguments with undefined.
    val mainArguments =
      this@J2clDefaultParameterInjector.context.jvmBackendContext.multiFieldValueClassReplacements
        .mapFunctionMfvcStructures(this, stubFunction, declaration) {
          sourceParameter: IrValueParameter,
          targetParameterType: IrType ->
          expression.arguments[sourceParameter.indexInParameters]?.maybeCoerceToNull()
            ?: nullConst(startOffset, endOffset, sourceParameter)
        }

    return buildMap {
      putAll(mainArguments)
      // The only remaining parameter to the stub function should be the default constructor marker,
      // if applicable.
      val restParameters = stubFunction.parameters.toSet().subtract(mainArguments.keys)
      if (restParameters.isNotEmpty()) {
        val lastParameter = restParameters.single()
        check(
          lastParameter.origin == IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER &&
            declaration is IrConstructor
        )
        put(lastParameter, nullConst(startOffset, endOffset, lastParameter.type))
      }
    }
  }

  private fun IrExpression.maybeCoerceToNull(): IrExpression {
    // There are trivial cases where we'll never see an undefined value:
    //   - primitive types
    //   - literal values
    if (type.isPrimitiveType() || this is IrConst) {
      return this
    }
    return IrCallImpl.fromSymbolOwner(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.intrinsics.jsCoerceToNullSymbol,
      )
      .apply {
        putTypeArgument(0, this@maybeCoerceToNull.type)
        putValueArgument(0, this@maybeCoerceToNull)
      }
  }
}

/**
 * Sets the default initializer of a parameter to be an error expression.
 *
 * This is intended to serve as a marker that the parameter is optional, but avoid anyone later on
 * from relying upon the contents. The actual initializers have been moved to the body of the
 * function.
 */
private fun IrValueParameter.stubDefaultValue(context: CommonBackendContext) {
  defaultValue =
    context.createIrBuilder(symbol).run {
      irExprBody(
        irErrorExpression(type, "Default stub").apply {
          if (defaultValue != null) {
            attributeOwnerId = defaultValue!!.expression
          }
        }
      )
    }
}

private fun IrBuilder.irErrorExpression(type: IrType, description: String) =
  IrErrorExpressionImpl(startOffset, endOffset, type, description)

private val IrFunction.hasDefaultParameters: Boolean
  get() = parameters.any { it.defaultValue != null }

private val IrFunction.hasDefaultBridge: Boolean
  get() = defaultArgumentsDispatchFunction != null && defaultArgumentsDispatchFunction != this
