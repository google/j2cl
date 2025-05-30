/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.ir

import kotlin.reflect.full.declaredMemberProperties
import org.jetbrains.kotlin.backend.common.linkage.issues.IrSymbolTypeMismatchException
import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFile
import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind.*
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBlock as ProtoBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBlockBody as ProtoBlockBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBranch as ProtoBranch
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBreak as ProtoBreak
import org.jetbrains.kotlin.backend.common.serialization.proto.IrCall as ProtoCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrCatch as ProtoCatch
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClassReference as ProtoClassReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrComposite as ProtoComposite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConst as ProtoConst
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConst.ValueCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrContinue as ProtoContinue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDelegatingConstructorCall as ProtoDelegatingConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDoWhile as ProtoDoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicMemberExpression as ProtoDynamicMemberExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicOperatorExpression as ProtoDynamicOperatorExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumConstructorCall as ProtoEnumConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorCallExpression as ProtoErrorCallExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorExpression as ProtoErrorExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionExpression as ProtoFunctionExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionReference as ProtoFunctionReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetClass as ProtoGetClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetEnumValue as ProtoGetEnumValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetField as ProtoGetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetObject as ProtoGetObject
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetValue as ProtoGetValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInlinedFunctionBlock as ProtoInlinedFunctionBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInstanceInitializerCall as ProtoInstanceInitializerCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedPropertyReference as ProtoLocalDelegatedPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation as ProtoOperation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation.OperationCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrPropertyReference as ProtoPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturn as ProtoReturn
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturnableBlock as ProtoReturnableBlock
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetField as ProtoSetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetValue as ProtoSetValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSpreadElement as ProtoSpreadElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement.StatementCase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStringConcat as ProtoStringConcat
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBody as ProtoSyntheticBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBodyKind as ProtoSyntheticBodyKind
import org.jetbrains.kotlin.backend.common.serialization.proto.IrThrow as ProtoThrow
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTry as ProtoTry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOp as ProtoTypeOp
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOperator as ProtoTypeOperator
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVararg as ProtoVararg
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVarargElement as ProtoVarargElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVarargElement.VarargElementCase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhen as ProtoWhen
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhile as ProtoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.Loop as ProtoLoop
import org.jetbrains.kotlin.backend.common.serialization.proto.MemberAccessCommon as ProtoMemberAccessCommon
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap

// Copied from org.jetbrains.kotlin.backend.jvm.serialization.IrBodyDeserializer to access internal
// api.
class IrBodyDeserializer(
  private val builtIns: IrBuiltIns,
  private val irFactory: IrFactory,
  private val libraryFile: IrLibraryFile,
  private val declarationDeserializer: IrDeclarationDeserializer,
  private val settings: IrDeserializationSettings,
) {

  private val fileLoops = hashMapOf<Int, IrLoop>()

  private fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoop): IrLoop =
    fileLoops.getOrPut(loopIndex, loopBuilder)

  private fun deserializeBlockBody(proto: ProtoBlockBody, start: Int, end: Int): IrBlockBody {
    val statements =
      proto.statementList.memoryOptimizedMap { deserializeStatement(it) as IrStatement }
    return irFactory.createBlockBody(start, end, statements)
  }

  private fun deserializeBranch(proto: ProtoBranch, start: Int, end: Int): IrBranch {

    val condition = deserializeExpression(proto.condition)
    val result = deserializeExpression(proto.result)

    return IrBranchImpl(start, end, condition, result)
  }

  private fun deserializeCatch(proto: ProtoCatch, start: Int, end: Int): IrCatch {
    val catchParameter = declarationDeserializer.deserializeIrVariable(proto.catchParameter)
    val result = deserializeExpression(proto.result)

    return IrCatchImpl(start, end, catchParameter, result)
  }

  private fun deserializeSyntheticBody(
    proto: ProtoSyntheticBody,
    start: Int,
    end: Int,
  ): IrSyntheticBody {
    val kind =
      when (proto.kind!!) {
        ProtoSyntheticBodyKind.ENUM_VALUES -> IrSyntheticBodyKind.ENUM_VALUES
        ProtoSyntheticBodyKind.ENUM_VALUEOF -> IrSyntheticBodyKind.ENUM_VALUEOF
        ProtoSyntheticBodyKind.ENUM_ENTRIES -> IrSyntheticBodyKind.ENUM_ENTRIES
      }
    return IrSyntheticBodyImpl(start, end, kind)
  }

  internal fun deserializeStatement(proto: ProtoStatement): IrElement {
    val coordinates = BinaryCoordinates.decode(proto.coordinates)
    val start = coordinates.startOffset
    val end = coordinates.endOffset
    val element =
      when (proto.statementCase) {
        StatementCase.BLOCK_BODY // proto.hasBlockBody()
        -> deserializeBlockBody(proto.blockBody, start, end)
        StatementCase.BRANCH // proto.hasBranch()
        -> deserializeBranch(proto.branch, start, end)
        StatementCase.CATCH // proto.hasCatch()
        -> deserializeCatch(proto.catch, start, end)
        StatementCase.DECLARATION // proto.hasDeclaration()
        -> declarationDeserializer.deserializeDeclaration(proto.declaration)
        StatementCase.EXPRESSION // proto.hasExpression()
        -> deserializeExpression(proto.expression)
        StatementCase.SYNTHETIC_BODY // proto.hasSyntheticBody()
        -> deserializeSyntheticBody(proto.syntheticBody, start, end)
        else -> TODO("Statement deserialization not implemented: ${proto.statementCase}")
      }

    return element
  }

  private fun deserializeBlock(proto: ProtoBlock, start: Int, end: Int, type: IrType): IrBlock {
    return withDeserializedBlock(proto) { origin, statements ->
      IrBlockImpl(start, end, type, origin, statements)
    }
  }

  private fun deserializeReturnableBlock(
    proto: ProtoReturnableBlock,
    start: Int,
    end: Int,
    type: IrType,
  ): IrReturnableBlock {
    val symbol =
      deserializeTypedSymbol<IrReturnableBlockSymbol>(proto.symbol, fallbackSymbolKind = null)
    return withDeserializedBlock(proto.base) { origin, statements ->
      IrReturnableBlockImpl(start, end, type, symbol, origin, statements)
    }
  }

  private fun deserializeInlinedFunctionBlock(
    proto: ProtoInlinedFunctionBlock,
    start: Int,
    end: Int,
    type: IrType,
  ): IrInlinedFunctionBlock {
    val inlinedFunctionSymbol =
      runIf(proto.hasInlinedFunctionSymbol()) {
        deserializeTypedSymbol<IrFunctionSymbol>(proto.inlinedFunctionSymbol, FUNCTION_SYMBOL)
      }
    val inlinedFunctionFileEntry = deserializeFileEntry(proto.inlinedFunctionFileEntry)
    return withDeserializedBlock(proto.base) { origin, statements ->
      IrInlinedFunctionBlockImpl(
        start,
        end,
        type,
        inlinedFunctionSymbol,
        proto.inlinedFunctionStartOffset,
        proto.inlinedFunctionEndOffset,
        inlinedFunctionFileEntry,
        origin,
        statements,
      )
    }
  }

  private inline fun <T> withDeserializedBlock(
    proto: ProtoBlock,
    block: (IrStatementOrigin?, List<IrStatement>) -> T,
  ): T {
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }
    val statements = proto.statementList.map { deserializeStatement(it) as IrStatement }

    return block(origin, statements)
  }

  private fun deserializeMemberAccessCommon(
    access: IrMemberAccessExpression<*>,
    proto: ProtoMemberAccessCommon,
  ) {

    proto.valueArgumentList.forEachIndexed { i, arg ->
      if (arg.hasExpression()) {
        val expr = deserializeExpression(arg.expression)
        access.putValueArgument(i, expr)
      }
    }

    proto.typeArgumentList.forEachIndexed { i, arg ->
      access.typeArguments[i] = declarationDeserializer.deserializeNullableIrType(arg)
    }

    if (proto.hasDispatchReceiver()) {
      access.dispatchReceiver = deserializeExpression(proto.dispatchReceiver)
    }
    if (proto.hasExtensionReceiver()) {
      access.extensionReceiver = deserializeExpression(proto.extensionReceiver)
    }
  }

  private fun deserializeClassReference(
    proto: ProtoClassReference,
    start: Int,
    end: Int,
    type: IrType,
  ): IrClassReference {
    val symbol =
      deserializeTypedSymbol<IrClassifierSymbol>(
        proto.classSymbol,
        fallbackSymbolKind = /* just the first possible option */ CLASS_SYMBOL,
      )
    val classType = declarationDeserializer.deserializeIrType(proto.classType)
    /** TODO: [createClassifierSymbolForClassReference] is internal function */
    return IrClassReferenceImpl(start, end, type, symbol, classType)
  }

  /**
   * This is a special lazy type for annotation. This type is computed using the symbol of the
   * annotation class that is retrieved through `IrConstructorCall.symbol.owner.parentAsClass`.
   *
   * The reason why this [IrAnnotationType] exists is that for some reason [IrConstructorCall]s
   * representing annotations are serialized just as [IrConstructorCall] and not as a part of
   * [IrExpression]. As far as types for expressions are always serialized as a part of
   * [IrExpression], there are no serialized types for annotations. So, the type needs to somehow be
   * restored given only [IrConstructorCall].
   *
   * TODO: Probably a bit more abstraction possible here up to [IrMemberAccessExpression] but at
   *   this point further complexization looks like overengineering.
   */
  private class IrAnnotationType : IrDelegatedSimpleType() {
    var irConstructorCall: IrConstructorCall? = null

    override val delegate: IrSimpleType by lazy { resolveType() }

    private fun resolveType(): IrSimpleType {
      val constructorCall =
        irConstructorCall ?: error("irConstructorCall should not be null at this stage")
      irConstructorCall = null

      val klass = constructorCall.symbol.owner.parentAsClass

      val typeParameters =
        extractTypeParameters(klass).ifEmpty {
          return IrSimpleTypeBuilder().apply { classifier = klass.symbol }.buildSimpleType()
        }

      val rawType =
        with(IrSimpleTypeBuilder()) {
          arguments =
            typeParameters.memoryOptimizedMap {
              classifier = it.symbol
              buildSimpleType()
            }
          classifier = klass.symbol
          buildSimpleType()
        }

      val typeParametersToArguments =
        HashMap<IrTypeParameterSymbol, IrTypeArgument>(typeParameters.size)
      for (i in typeParameters.indices) {
        val typeParameter = typeParameters[i]
        val callTypeArgument =
          constructorCall.typeArguments[i] ?: error("No type argument for id $i")
        val typeArgument = makeTypeProjection(callTypeArgument, typeParameter.variance)
        typeParametersToArguments[typeParameter.symbol] = typeArgument
      }

      val substitutor = IrTypeSubstitutor(typeParametersToArguments)
      return substitutor.substitute(rawType) as IrSimpleType
    }
  }

  fun deserializeAnnotation(proto: ProtoConstructorCall): IrConstructorCall {
    // TODO: use real coordinates
    val startOffset = 0
    val endOffset = 0

    if (settings.useNullableAnyAsAnnotationConstructorCallType)
      return deserializeConstructorCall(proto, startOffset, endOffset, builtIns.anyNType)
    else {
      val irType = IrAnnotationType()
      return deserializeConstructorCall(proto, startOffset, endOffset, irType).also {
        irType.irConstructorCall = it
      }
    }
  }

  private fun deserializeConstructorCall(
    proto: ProtoConstructorCall,
    start: Int,
    end: Int,
    type: IrType,
  ): IrConstructorCall {
    val symbol = deserializeTypedSymbol<IrConstructorSymbol>(proto.symbol, CONSTRUCTOR_SYMBOL)
    return IrConstructorCallImplWithShape(
        start,
        end,
        type,
        symbol,
        typeArgumentsCount = proto.memberAccess.typeArgumentCount,
        constructorTypeArgumentsCount = proto.constructorTypeArgumentsCount,
        valueArgumentsCount = proto.memberAccess.valueArgumentCount,
        contextParameterCount = 0,
        hasDispatchReceiver = proto.memberAccess.hasDispatchReceiver(),
        hasExtensionReceiver = proto.memberAccess.hasExtensionReceiver(),
        origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName },
      )
      .also { deserializeMemberAccessCommon(it, proto.memberAccess) }
  }

  private fun deserializeCall(proto: ProtoCall, start: Int, end: Int, type: IrType): IrCall {
    val symbol = deserializeTypedSymbol<IrSimpleFunctionSymbol>(proto.symbol, FUNCTION_SYMBOL)
    val superSymbol =
      deserializeTypedSymbolWhen<IrClassSymbol>(proto.hasSuper(), CLASS_SYMBOL) { proto.`super` }
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }

    val call: IrCall =
      // TODO: implement the last three args here.
      IrCallImplWithShape(
        start,
        end,
        type,
        symbol,
        proto.memberAccess.typeArgumentCount,
        proto.memberAccess.valueArgumentList.size,
        contextParameterCount = 0,
        hasDispatchReceiver = proto.memberAccess.hasDispatchReceiver(),
        hasExtensionReceiver = proto.memberAccess.hasExtensionReceiver(),
        origin,
        superSymbol,
      )
    deserializeMemberAccessCommon(call, proto.memberAccess)
    return call
  }

  private fun deserializeComposite(
    proto: ProtoComposite,
    start: Int,
    end: Int,
    type: IrType,
  ): IrComposite {
    val statements = mutableListOf<IrStatement>()
    val statementProtos = proto.statementList
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }

    statementProtos.forEach { statements.add(deserializeStatement(it) as IrStatement) }
    return IrCompositeImpl(start, end, type, origin, statements)
  }

  private fun deserializeDelegatingConstructorCall(
    proto: ProtoDelegatingConstructorCall,
    start: Int,
    end: Int,
  ): IrDelegatingConstructorCall {
    val symbol = deserializeTypedSymbol<IrConstructorSymbol>(proto.symbol, CONSTRUCTOR_SYMBOL)
    val call =
      IrDelegatingConstructorCallImplWithShape(
        start,
        end,
        builtIns.unitType,
        symbol,
        proto.memberAccess.typeArgumentCount,
        proto.memberAccess.valueArgumentCount,
        contextParameterCount = 0,
        hasDispatchReceiver = proto.memberAccess.hasDispatchReceiver(),
        hasExtensionReceiver = proto.memberAccess.hasExtensionReceiver(),
      )

    deserializeMemberAccessCommon(call, proto.memberAccess)
    return call
  }

  private fun deserializeEnumConstructorCall(
    proto: ProtoEnumConstructorCall,
    start: Int,
    end: Int,
  ): IrEnumConstructorCall {
    val symbol = deserializeTypedSymbol<IrConstructorSymbol>(proto.symbol, CONSTRUCTOR_SYMBOL)
    val call =
      IrEnumConstructorCallImplWithShape(
        start,
        end,
        builtIns.unitType,
        symbol,
        proto.memberAccess.typeArgumentCount,
        proto.memberAccess.valueArgumentCount,
        contextParameterCount = 0,
        hasDispatchReceiver = proto.memberAccess.hasDispatchReceiver(),
        hasExtensionReceiver = proto.memberAccess.hasExtensionReceiver(),
      )
    deserializeMemberAccessCommon(call, proto.memberAccess)
    return call
  }

  private fun deserializeFunctionExpression(
    functionExpression: ProtoFunctionExpression,
    start: Int,
    end: Int,
    type: IrType,
  ) =
    IrFunctionExpressionImpl(
      start,
      end,
      type,
      declarationDeserializer.deserializeIrFunction(functionExpression.function),
      deserializeIrStatementOrigin(functionExpression.originName),
    )

  private fun deserializeErrorExpression(
    proto: ProtoErrorExpression,
    start: Int,
    end: Int,
    type: IrType,
  ): IrErrorExpression {
    require(settings.allowErrorNodes) {
      "IrErrorExpression($start, $end, \"${libraryFile.string(proto.description)}\") found but error code is not allowed"
    }
    return IrErrorExpressionImpl(start, end, type, libraryFile.string(proto.description))
  }

  private fun deserializeErrorCallExpression(
    proto: ProtoErrorCallExpression,
    start: Int,
    end: Int,
    type: IrType,
  ): IrErrorCallExpression {
    require(settings.allowErrorNodes) {
      "IrErrorCallExpressionImpl($start, $end, \"${libraryFile.string(proto.description)}\") found but error code is not allowed"
    }
    return IrErrorCallExpressionImpl(start, end, type, libraryFile.string(proto.description))
      .apply {
        if (proto.hasReceiver()) {
          explicitReceiver = deserializeExpression(proto.receiver)
        }
        proto.valueArgumentList.forEach { arguments.add(deserializeExpression(it)) }
      }
  }

  private fun deserializeFunctionReference(
    proto: ProtoFunctionReference,
    start: Int,
    end: Int,
    type: IrType,
  ): IrFunctionReference {

    val symbol =
      deserializeTypedSymbol<IrFunctionSymbol>(
        proto.symbol,
        fallbackSymbolKind = /* just the first possible option */ FUNCTION_SYMBOL,
      )
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }
    val reflectionTarget =
      deserializeTypedSymbolWhen<IrFunctionSymbol>(
        proto.hasReflectionTargetSymbol(),
        fallbackSymbolKind = /* just the first possible option */ FUNCTION_SYMBOL,
      ) {
        proto.reflectionTargetSymbol
      }
    val callable =
      IrFunctionReferenceImplWithShape(
        start,
        end,
        type,
        symbol,
        proto.memberAccess.typeArgumentCount,
        proto.memberAccess.valueArgumentCount,
        contextParameterCount = 0,
        hasDispatchReceiver = proto.memberAccess.hasDispatchReceiver(),
        hasExtensionReceiver = proto.memberAccess.hasExtensionReceiver(),
        reflectionTarget,
        origin,
      )
    deserializeMemberAccessCommon(callable, proto.memberAccess)

    return callable
  }

  private fun deserializeGetClass(
    proto: ProtoGetClass,
    start: Int,
    end: Int,
    type: IrType,
  ): IrGetClass {
    val argument = deserializeExpression(proto.argument)
    return IrGetClassImpl(start, end, type, argument)
  }

  private fun deserializeGetField(
    proto: ProtoGetField,
    start: Int,
    end: Int,
    type: IrType,
  ): IrGetField {
    val access = proto.fieldAccess
    val symbol = deserializeTypedSymbol<IrFieldSymbol>(access.symbol, FIELD_SYMBOL)
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }
    val superQualifier =
      deserializeTypedSymbolWhen<IrClassSymbol>(access.hasSuper(), CLASS_SYMBOL) { access.`super` }
    val receiver =
      if (access.hasReceiver()) {
        deserializeExpression(access.receiver)
      } else null

    return IrGetFieldImpl(start, end, symbol, type, receiver, origin, superQualifier)
  }

  private fun deserializeGetValue(
    proto: ProtoGetValue,
    start: Int,
    end: Int,
    type: IrType,
  ): IrGetValue {
    val symbol = deserializeTypedSymbol<IrValueSymbol>(proto.symbol, fallbackSymbolKind = null)
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }
    // TODO: origin!
    return IrGetValueImpl(start, end, type, symbol, origin)
  }

  private fun deserializeGetEnumValue(
    proto: ProtoGetEnumValue,
    start: Int,
    end: Int,
    type: IrType,
  ): IrGetEnumValue {
    val symbol = deserializeTypedSymbol<IrEnumEntrySymbol>(proto.symbol, ENUM_ENTRY_SYMBOL)
    return IrGetEnumValueImpl(start, end, type, symbol)
  }

  private fun deserializeGetObject(
    proto: ProtoGetObject,
    start: Int,
    end: Int,
    type: IrType,
  ): IrGetObjectValue {
    val symbol = deserializeTypedSymbol<IrClassSymbol>(proto.symbol, CLASS_SYMBOL)
    return IrGetObjectValueImpl(start, end, type, symbol)
  }

  private fun deserializeInstanceInitializerCall(
    proto: ProtoInstanceInitializerCall,
    start: Int,
    end: Int,
  ): IrInstanceInitializerCall {
    val symbol = deserializeTypedSymbol<IrClassSymbol>(proto.symbol, CLASS_SYMBOL)
    return IrInstanceInitializerCallImpl(start, end, symbol, builtIns.unitType)
  }

  private fun deserializeIrLocalDelegatedPropertyReference(
    proto: ProtoLocalDelegatedPropertyReference,
    start: Int,
    end: Int,
    type: IrType,
  ): IrLocalDelegatedPropertyReference {

    val delegate =
      deserializeTypedSymbol<IrVariableSymbol>(proto.delegate, fallbackSymbolKind = null)
    val getter = deserializeTypedSymbol<IrSimpleFunctionSymbol>(proto.getter, FUNCTION_SYMBOL)
    val setter =
      deserializeTypedSymbolWhen<IrSimpleFunctionSymbol>(proto.hasSetter(), FUNCTION_SYMBOL) {
        proto.setter
      }
    val symbol =
      deserializeTypedSymbol<IrLocalDelegatedPropertySymbol>(
        proto.symbol,
        fallbackSymbolKind = null,
      )
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }

    return IrLocalDelegatedPropertyReferenceImpl(
      start,
      end,
      type,
      symbol,
      delegate,
      getter,
      setter,
      origin,
    )
  }

  private fun deserializePropertyReference(
    proto: ProtoPropertyReference,
    start: Int,
    end: Int,
    type: IrType,
  ): IrPropertyReference {
    val symbol = deserializeTypedSymbol<IrPropertySymbol>(proto.symbol, PROPERTY_SYMBOL)
    val field =
      deserializeTypedSymbolWhen<IrFieldSymbol>(proto.hasField(), FIELD_SYMBOL) { proto.field }
    val getter =
      deserializeTypedSymbolWhen<IrSimpleFunctionSymbol>(proto.hasGetter(), FUNCTION_SYMBOL) {
        proto.getter
      }
    val setter =
      deserializeTypedSymbolWhen<IrSimpleFunctionSymbol>(proto.hasSetter(), FUNCTION_SYMBOL) {
        proto.setter
      }

    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }

    val callable =
      IrPropertyReferenceImplWithShape(
        start,
        end,
        type,
        symbol,
        proto.memberAccess.hasDispatchReceiver(),
        proto.memberAccess.hasExtensionReceiver(),
        proto.memberAccess.typeArgumentCount,
        field,
        getter,
        setter,
        origin,
      )
    deserializeMemberAccessCommon(callable, proto.memberAccess)
    return callable
  }

  private fun deserializeReturn(proto: ProtoReturn, start: Int, end: Int): IrReturn {
    val symbol =
      deserializeTypedSymbol<IrReturnTargetSymbol>(
        proto.returnTarget,
        fallbackSymbolKind = /* just the first possible option */ FUNCTION_SYMBOL,
      )
    val value = deserializeExpression(proto.value)
    return IrReturnImpl(start, end, builtIns.nothingType, symbol, value)
  }

  private fun deserializeSetField(proto: ProtoSetField, start: Int, end: Int): IrSetField {
    val access = proto.fieldAccess
    val symbol = deserializeTypedSymbol<IrFieldSymbol>(access.symbol, FIELD_SYMBOL)
    val superQualifier =
      deserializeTypedSymbolWhen<IrClassSymbol>(access.hasSuper(), CLASS_SYMBOL) { access.`super` }
    val receiver =
      if (access.hasReceiver()) {
        deserializeExpression(access.receiver)
      } else null
    val value = deserializeExpression(proto.value)
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }

    return IrSetFieldImpl(
      start,
      end,
      symbol,
      receiver,
      value,
      builtIns.unitType,
      origin,
      superQualifier,
    )
  }

  private fun deserializeSetValue(proto: ProtoSetValue, start: Int, end: Int): IrSetValue {
    val symbol = deserializeTypedSymbol<IrValueSymbol>(proto.symbol, fallbackSymbolKind = null)
    val value = deserializeExpression(proto.value)
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }
    return IrSetValueImpl(start, end, builtIns.unitType, symbol, value, origin)
  }

  private fun deserializeSpreadElement(proto: ProtoSpreadElement): IrSpreadElement {
    val expression = deserializeExpression(proto.expression)
    val coordinates = BinaryCoordinates.decode(proto.coordinates)
    return IrSpreadElementImpl(coordinates.startOffset, coordinates.endOffset, expression)
  }

  private fun deserializeStringConcat(
    proto: ProtoStringConcat,
    start: Int,
    end: Int,
    type: IrType,
  ): IrStringConcatenation {
    val argumentProtos = proto.argumentList
    val arguments = mutableListOf<IrExpression>()

    argumentProtos.forEach { arguments.add(deserializeExpression(it)) }
    return IrStringConcatenationImpl(start, end, type, arguments)
  }

  private fun deserializeThrow(proto: ProtoThrow, start: Int, end: Int): IrThrowImpl {
    return IrThrowImpl(start, end, builtIns.nothingType, deserializeExpression(proto.value))
  }

  private fun deserializeTry(proto: ProtoTry, start: Int, end: Int, type: IrType): IrTryImpl {
    val result = deserializeExpression(proto.result)
    val catches = mutableListOf<IrCatch>()
    proto.catchList.forEach { catches.add(deserializeStatement(it) as IrCatch) }
    val finallyExpression = if (proto.hasFinally()) deserializeExpression(proto.finally) else null
    return IrTryImpl(start, end, type, result, catches, finallyExpression)
  }

  private fun deserializeTypeOperator(operator: ProtoTypeOperator) =
    when (operator) {
      ProtoTypeOperator.CAST -> IrTypeOperator.CAST
      ProtoTypeOperator.IMPLICIT_CAST -> IrTypeOperator.IMPLICIT_CAST
      ProtoTypeOperator.IMPLICIT_NOTNULL -> IrTypeOperator.IMPLICIT_NOTNULL
      ProtoTypeOperator.IMPLICIT_COERCION_TO_UNIT -> IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
      ProtoTypeOperator.IMPLICIT_INTEGER_COERCION -> IrTypeOperator.IMPLICIT_INTEGER_COERCION
      ProtoTypeOperator.SAFE_CAST -> IrTypeOperator.SAFE_CAST
      ProtoTypeOperator.INSTANCEOF -> IrTypeOperator.INSTANCEOF
      ProtoTypeOperator.NOT_INSTANCEOF -> IrTypeOperator.NOT_INSTANCEOF
      ProtoTypeOperator.SAM_CONVERSION -> IrTypeOperator.SAM_CONVERSION
      ProtoTypeOperator.IMPLICIT_DYNAMIC_CAST -> IrTypeOperator.IMPLICIT_DYNAMIC_CAST
      ProtoTypeOperator.REINTERPRET_CAST -> IrTypeOperator.REINTERPRET_CAST
    }

  private fun deserializeTypeOp(
    proto: ProtoTypeOp,
    start: Int,
    end: Int,
    type: IrType,
  ): IrTypeOperatorCall {
    val operator = deserializeTypeOperator(proto.operator)
    val operand = declarationDeserializer.deserializeIrType(proto.operand) // .brokenIr
    val argument = deserializeExpression(proto.argument)
    return IrTypeOperatorCallImpl(start, end, type, operator, operand, argument)
  }

  private fun deserializeVararg(proto: ProtoVararg, start: Int, end: Int, type: IrType): IrVararg {
    val elementType = declarationDeserializer.deserializeIrType(proto.elementType)

    val elements = mutableListOf<IrVarargElement>()
    proto.elementList.forEach { elements.add(deserializeVarargElement(it)) }
    return IrVarargImpl(start, end, type, elementType, elements)
  }

  private fun deserializeVarargElement(element: ProtoVarargElement): IrVarargElement {
    return when (element.varargElementCase) {
      VarargElementCase.EXPRESSION -> deserializeExpression(element.expression)
      VarargElementCase.SPREAD_ELEMENT -> deserializeSpreadElement(element.spreadElement)
      else -> TODO("Unexpected vararg element")
    }
  }

  private fun deserializeWhen(proto: ProtoWhen, start: Int, end: Int, type: IrType): IrWhen {
    val branches = mutableListOf<IrBranch>()
    val origin = deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName }

    proto.branchList.forEach { branches.add(deserializeStatement(it) as IrBranch) }

    // TODO: provide some origin!
    return IrWhenImpl(start, end, type, origin, branches)
  }

  private fun deserializeLoop(proto: ProtoLoop, loop: IrLoop): IrLoop {
    val label = if (proto.hasLabel()) libraryFile.string(proto.label) else null
    val body = if (proto.hasBody()) deserializeExpression(proto.body) else null
    val condition = deserializeExpression(proto.condition)

    loop.label = label
    loop.condition = condition
    loop.body = body

    return loop
  }

  // we create the loop before deserializing the body, so that
  // IrBreak statements have something to put into 'loop' field.
  private fun deserializeDoWhile(proto: ProtoDoWhile, start: Int, end: Int, type: IrType) =
    deserializeLoop(
      proto.loop,
      deserializeLoopHeader(proto.loop.loopId) {
        val origin =
          deserializeIrStatementOrigin(proto.loop.hasOriginName()) { proto.loop.originName }
        IrDoWhileLoopImpl(start, end, type, origin)
      },
    )

  private fun deserializeWhile(proto: ProtoWhile, start: Int, end: Int, type: IrType) =
    deserializeLoop(
      proto.loop,
      deserializeLoopHeader(proto.loop.loopId) {
        val origin =
          deserializeIrStatementOrigin(proto.loop.hasOriginName()) { proto.loop.originName }
        IrWhileLoopImpl(start, end, type, origin)
      },
    )

  private fun deserializeDynamicMemberExpression(
    proto: ProtoDynamicMemberExpression,
    start: Int,
    end: Int,
    type: IrType,
  ) =
    IrDynamicMemberExpressionImpl(
      start,
      end,
      type,
      libraryFile.string(proto.memberName),
      deserializeExpression(proto.receiver),
    )

  private fun deserializeDynamicOperatorExpression(
    proto: ProtoDynamicOperatorExpression,
    start: Int,
    end: Int,
    type: IrType,
  ) =
    IrDynamicOperatorExpressionImpl(start, end, type, deserializeDynamicOperator(proto.operator))
      .apply {
        receiver = deserializeExpression(proto.receiver)
        proto.argumentList.mapTo(arguments) { deserializeExpression(it) }
      }

  private fun deserializeDynamicOperator(
    operator: ProtoDynamicOperatorExpression.IrDynamicOperator
  ) =
    when (operator) {
      ProtoDynamicOperatorExpression.IrDynamicOperator.UNARY_PLUS -> IrDynamicOperator.UNARY_PLUS
      ProtoDynamicOperatorExpression.IrDynamicOperator.UNARY_MINUS -> IrDynamicOperator.UNARY_MINUS

      ProtoDynamicOperatorExpression.IrDynamicOperator.EXCL -> IrDynamicOperator.EXCL

      ProtoDynamicOperatorExpression.IrDynamicOperator.PREFIX_INCREMENT ->
        IrDynamicOperator.PREFIX_INCREMENT
      ProtoDynamicOperatorExpression.IrDynamicOperator.PREFIX_DECREMENT ->
        IrDynamicOperator.PREFIX_DECREMENT

      ProtoDynamicOperatorExpression.IrDynamicOperator.POSTFIX_INCREMENT ->
        IrDynamicOperator.POSTFIX_INCREMENT
      ProtoDynamicOperatorExpression.IrDynamicOperator.POSTFIX_DECREMENT ->
        IrDynamicOperator.POSTFIX_DECREMENT

      ProtoDynamicOperatorExpression.IrDynamicOperator.BINARY_PLUS -> IrDynamicOperator.BINARY_PLUS
      ProtoDynamicOperatorExpression.IrDynamicOperator.BINARY_MINUS ->
        IrDynamicOperator.BINARY_MINUS
      ProtoDynamicOperatorExpression.IrDynamicOperator.MUL -> IrDynamicOperator.MUL
      ProtoDynamicOperatorExpression.IrDynamicOperator.DIV -> IrDynamicOperator.DIV
      ProtoDynamicOperatorExpression.IrDynamicOperator.MOD -> IrDynamicOperator.MOD

      ProtoDynamicOperatorExpression.IrDynamicOperator.GT -> IrDynamicOperator.GT
      ProtoDynamicOperatorExpression.IrDynamicOperator.LT -> IrDynamicOperator.LT
      ProtoDynamicOperatorExpression.IrDynamicOperator.GE -> IrDynamicOperator.GE
      ProtoDynamicOperatorExpression.IrDynamicOperator.LE -> IrDynamicOperator.LE

      ProtoDynamicOperatorExpression.IrDynamicOperator.EQEQ -> IrDynamicOperator.EQEQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.EXCLEQ -> IrDynamicOperator.EXCLEQ

      ProtoDynamicOperatorExpression.IrDynamicOperator.EQEQEQ -> IrDynamicOperator.EQEQEQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.EXCLEQEQ -> IrDynamicOperator.EXCLEQEQ

      ProtoDynamicOperatorExpression.IrDynamicOperator.ANDAND -> IrDynamicOperator.ANDAND
      ProtoDynamicOperatorExpression.IrDynamicOperator.OROR -> IrDynamicOperator.OROR

      ProtoDynamicOperatorExpression.IrDynamicOperator.EQ -> IrDynamicOperator.EQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.PLUSEQ -> IrDynamicOperator.PLUSEQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.MINUSEQ -> IrDynamicOperator.MINUSEQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.MULEQ -> IrDynamicOperator.MULEQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.DIVEQ -> IrDynamicOperator.DIVEQ
      ProtoDynamicOperatorExpression.IrDynamicOperator.MODEQ -> IrDynamicOperator.MODEQ

      ProtoDynamicOperatorExpression.IrDynamicOperator.ARRAY_ACCESS ->
        IrDynamicOperator.ARRAY_ACCESS

      ProtoDynamicOperatorExpression.IrDynamicOperator.INVOKE -> IrDynamicOperator.INVOKE
    }

  private fun deserializeBreak(proto: ProtoBreak, start: Int, end: Int, type: IrType): IrBreak {
    val label = if (proto.hasLabel()) libraryFile.string(proto.label) else null
    val loopId = proto.loopId
    val loop = deserializeLoopHeader(loopId) { error("break clause before loop header") }
    val irBreak = IrBreakImpl(start, end, type, loop)
    irBreak.label = label

    return irBreak
  }

  private fun deserializeContinue(
    proto: ProtoContinue,
    start: Int,
    end: Int,
    type: IrType,
  ): IrContinue {
    val label = if (proto.hasLabel()) libraryFile.string(proto.label) else null
    val loopId = proto.loopId
    val loop = deserializeLoopHeader(loopId) { error("break clause before loop header") }
    val irContinue = IrContinueImpl(start, end, type, loop)
    irContinue.label = label

    return irContinue
  }

  private fun deserializeConst(
    proto: ProtoConst,
    start: Int,
    end: Int,
    type: IrType,
  ): IrExpression =
    when (proto.valueCase!!) {
      NULL -> IrConstImpl.constNull(start, end, type)
      BOOLEAN -> IrConstImpl.boolean(start, end, type, proto.boolean)
      BYTE -> IrConstImpl.byte(start, end, type, proto.byte.toByte())
      CHAR -> IrConstImpl.char(start, end, type, proto.char.toChar())
      SHORT -> IrConstImpl.short(start, end, type, proto.short.toShort())
      INT -> IrConstImpl.int(start, end, type, proto.int)
      LONG -> IrConstImpl.long(start, end, type, proto.long)
      STRING -> IrConstImpl.string(start, end, type, libraryFile.string(proto.string))
      FLOAT_BITS -> IrConstImpl.float(start, end, type, Float.fromBits(proto.floatBits))
      DOUBLE_BITS -> IrConstImpl.double(start, end, type, Double.fromBits(proto.doubleBits))
      VALUE_NOT_SET -> error("Const deserialization error: ${proto.valueCase} ")
    }

  private fun deserializeOperation(
    proto: ProtoOperation,
    start: Int,
    end: Int,
    type: IrType,
  ): IrExpression =
    when (proto.operationCase!!) {
      BLOCK -> deserializeBlock(proto.block, start, end, type)
      RETURNABLE_BLOCK -> deserializeReturnableBlock(proto.returnableBlock, start, end, type)
      INLINED_FUNCTION_BLOCK ->
        deserializeInlinedFunctionBlock(proto.inlinedFunctionBlock, start, end, type)
      BREAK -> deserializeBreak(proto.`break`, start, end, type)
      CLASS_REFERENCE -> deserializeClassReference(proto.classReference, start, end, type)
      CALL -> deserializeCall(proto.call, start, end, type)
      COMPOSITE -> deserializeComposite(proto.composite, start, end, type)
      CONST -> deserializeConst(proto.const, start, end, type)
      CONTINUE -> deserializeContinue(proto.`continue`, start, end, type)
      DELEGATING_CONSTRUCTOR_CALL ->
        deserializeDelegatingConstructorCall(proto.delegatingConstructorCall, start, end)
      DO_WHILE -> deserializeDoWhile(proto.doWhile, start, end, type)
      ENUM_CONSTRUCTOR_CALL -> deserializeEnumConstructorCall(proto.enumConstructorCall, start, end)
      FUNCTION_REFERENCE -> deserializeFunctionReference(proto.functionReference, start, end, type)
      GET_ENUM_VALUE -> deserializeGetEnumValue(proto.getEnumValue, start, end, type)
      GET_CLASS -> deserializeGetClass(proto.getClass, start, end, type)
      GET_FIELD -> deserializeGetField(proto.getField, start, end, type)
      GET_OBJECT -> deserializeGetObject(proto.getObject, start, end, type)
      GET_VALUE -> deserializeGetValue(proto.getValue, start, end, type)
      LOCAL_DELEGATED_PROPERTY_REFERENCE ->
        deserializeIrLocalDelegatedPropertyReference(
          proto.localDelegatedPropertyReference,
          start,
          end,
          type,
        )
      INSTANCE_INITIALIZER_CALL ->
        deserializeInstanceInitializerCall(proto.instanceInitializerCall, start, end)
      PROPERTY_REFERENCE -> deserializePropertyReference(proto.propertyReference, start, end, type)
      RETURN -> deserializeReturn(proto.`return`, start, end)
      SET_FIELD -> deserializeSetField(proto.setField, start, end)
      SET_VALUE -> deserializeSetValue(proto.setValue, start, end)
      STRING_CONCAT -> deserializeStringConcat(proto.stringConcat, start, end, type)
      THROW -> deserializeThrow(proto.`throw`, start, end)
      TRY -> deserializeTry(proto.`try`, start, end, type)
      TYPE_OP -> deserializeTypeOp(proto.typeOp, start, end, type)
      VARARG -> deserializeVararg(proto.vararg, start, end, type)
      WHEN -> deserializeWhen(proto.`when`, start, end, type)
      WHILE -> deserializeWhile(proto.`while`, start, end, type)
      DYNAMIC_MEMBER -> deserializeDynamicMemberExpression(proto.dynamicMember, start, end, type)
      DYNAMIC_OPERATOR ->
        deserializeDynamicOperatorExpression(proto.dynamicOperator, start, end, type)
      CONSTRUCTOR_CALL -> deserializeConstructorCall(proto.constructorCall, start, end, type)
      FUNCTION_EXPRESSION ->
        deserializeFunctionExpression(proto.functionExpression, start, end, type)
      ERROR_EXPRESSION -> deserializeErrorExpression(proto.errorExpression, start, end, type)
      ERROR_CALL_EXPRESSION ->
        deserializeErrorCallExpression(proto.errorCallExpression, start, end, type)
      OPERATION_NOT_SET ->
        error("Expression deserialization not implemented: ${proto.operationCase}")
    }

  fun deserializeExpression(proto: ProtoExpression): IrExpression {
    val coordinates = BinaryCoordinates.decode(proto.coordinates)
    val start = coordinates.startOffset
    val end = coordinates.endOffset
    val type = declarationDeserializer.deserializeIrType(proto.type)
    val operation = proto.operation
    val expression = deserializeOperation(operation, start, end, type)

    return expression
  }

  private fun deserializeIrStatementOrigin(protoName: Int): IrStatementOrigin {
    val originName = libraryFile.string(protoName)
    val componentPrefix = "COMPONENT_"

    return if (originName.startsWith(componentPrefix))
      IrStatementOrigin.COMPONENT_N.withIndex(originName.removePrefix(componentPrefix).toInt())
    else statementOriginIndex[originName] ?: error("Unexpected statement origin: $originName")
  }

  /**
   * This is more compact form of deserializeIrStatementOrigin() that allows writing val origin =
   * deserializeIrStatementOrigin(proto.hasOriginName()) { proto.originName } instead of (as it was
   * before) val origin = if (proto.hasOriginName()) deserializeIrStatementOrigin(proto.originName)
   * else null
   */
  private inline fun deserializeIrStatementOrigin(
    hasOriginName: Boolean,
    protoName: () -> Int,
  ): IrStatementOrigin? = if (hasOriginName) deserializeIrStatementOrigin(protoName()) else null

  /**
   * This function allows to check deserialized symbols. If the deserialized symbol mismatches the
   * symbol kind at the call site in the deserializer then generate and reference another symbol
   * with the same signature. In case PL is off, just throw [IrSymbolTypeMismatchException].
   *
   * Note: [fallbackSymbolKind] must not completely match [S], but it should represent a subclass of
   * [S].
   *
   * Example: [S] is [IrClassifierSymbol] and [fallbackSymbolKind] is [CLASS_SYMBOL], which is only
   * one possible option along with [TYPE_PARAMETER_SYMBOL].
   *
   * Note, that for local IR declarations such as [IrValueDeclaration] [fallbackSymbolKind] can be
   * left null.
   */
  private inline fun <reified S : IrSymbol> deserializeTypedSymbol(
    code: Long,
    fallbackSymbolKind: SymbolKind?,
  ): S =
    with(declarationDeserializer) {
      val symbol = deserializeIrSymbol(code)
      symbol.checkSymbolType(fallbackSymbolKind)
    }

  private inline fun <reified S : IrSymbol> deserializeTypedSymbolWhen(
    condition: Boolean,
    fallbackSymbolKind: SymbolKind?,
    code: () -> Long,
  ): S? = if (condition) deserializeTypedSymbol(code(), fallbackSymbolKind) else null

  private companion object {
    private val statementOriginIndex =
      IrStatementOrigin.Companion::class
        .declaredMemberProperties
        .mapNotNull { it.get(IrStatementOrigin.Companion) as? IrStatementOrigin }
        .associateBy { it.debugName }
  }
}

// MODIFIED BY GOOGLE.
// Copied from org.jetbrains.kotlin.backend.jvm.serialization.IrFileDeserializer to make internal
// api available.
internal fun deserializeFileEntry(fileEntryProto: ProtoFileEntry): IrFileEntry {
  val fileName = fileEntryProto.name
  return NaiveSourceBasedFileEntryImpl(fileName, fileEntryProto.lineStartOffsetList.toIntArray())
}
// END OF MODIFICATIONS.
