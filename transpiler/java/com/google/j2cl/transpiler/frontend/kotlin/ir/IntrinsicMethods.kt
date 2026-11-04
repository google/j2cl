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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")
@file:OptIn(UnsafeDuringIrConstructionAPI::class, InternalSymbolFinderAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.ir

import com.google.j2cl.transpiler.ast.BinaryOperator
import com.google.j2cl.transpiler.ast.PrefixOperator
import org.jetbrains.kotlin.backend.common.ir.BackendSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * KotlinC uses intrinsics methods for representing some specific operations. The implementations of
 * these intrisics methods only known by the compiler and depends on the platform the code is
 * compiled to.
 *
 * <p> We need to specifically handle those methods in order to map them to the right operation in
 * the J2CL AST.
 *
 * <p> The list on intrinsic methods in KoltinC can be found in http://shortn/_7YGQ0Kch3v
 */
class IntrinsicMethods(private val irBuiltIns: IrBuiltIns, private val symbols: BackendSymbols) {
  private val kotlinFqn = StandardNames.BUILT_INS_PACKAGE_FQ_NAME
  private val kotlinJvmFqn = JvmStandardClassIds.BASE_JVM_PACKAGE

  private fun topLevelGetterSymbol(
    packageName: FqName,
    propertyName: String,
  ): IrSimpleFunctionSymbol =
    irBuiltIns.symbolFinder
      .findProperties(CallableId(packageName, Name.identifier(propertyName)))
      .single()
      .owner
      .getter!!
      .symbol

  private fun getterSymbol(classId: ClassId, propertyName: String): IrSimpleFunctionSymbol =
    checkNotNull(classSymbol(classId).getPropertyGetter(propertyName)) {
      "Property $propertyName not found in $classId"
    }

  private fun topLevelFunctionSymbol(packageFqName: FqName, name: String): IrSimpleFunctionSymbol =
    irBuiltIns.symbolFinder.findFunctions(CallableId(packageFqName, Name.identifier(name))).single()

  private fun functionSymbol(
    classId: ClassId,
    name: String,
    condition: (IrSimpleFunction) -> Boolean = { true },
  ): IrSimpleFunctionSymbol = functionSymbols(classId, name).single { condition(it.owner) }

  private fun functionSymbols(classId: ClassId, name: String): List<IrSimpleFunctionSymbol> =
    classSymbol(classId).functions.filter { it.owner.name.asString() == name }.toList()

  private fun classSymbol(classId: ClassId): IrClassSymbol =
    checkNotNull(irBuiltIns.symbolFinder.findClass(classId)) { "Class $classId not found" }

  /** The class ids of `Array` and of the primitive arrays (e.g. `IntArray`). */
  private val arrayClassIds =
    StandardClassIds.primitiveArrayTypeByElementType.values + StandardClassIds.Array

  private val arraySizeGetterSymbols: Set<IrFunctionSymbol> by lazy {
    arrayClassIds.mapTo(HashSet()) { getterSymbol(it, "size") }
  }

  private fun arrayMemberFunctionSymbols(name: String): Set<IrFunctionSymbol> =
    arrayClassIds.mapTo(HashSet()) { functionSymbol(it, name) }

  private val arrayGetSymbols by lazy { arrayMemberFunctionSymbols("get") }
  private val arraySetSymbols by lazy { arrayMemberFunctionSymbols("set") }
  private val arrayIteratorSymbols by lazy { arrayMemberFunctionSymbols("iterator") }

  /**
   * The constructors of the array classes. Note that `Array` has no primary constructor and that
   * the primitive arrays have two constructors (`IntArray(size)` and `IntArray(size, init)`).
   */
  private val arrayConstructorSymbols: Set<IrFunctionSymbol> by lazy {
    arrayClassIds.flatMapTo(HashSet()) { classSymbol(it).constructors }
  }

  fun isNewArray(irConstructorCall: IrConstructorCall): Boolean =
    irConstructorCall.symbol in arrayConstructorSymbols

  fun isArraySize(irCall: IrCall): Boolean = irCall.symbol in arraySizeGetterSymbols

  fun isArrayGet(irCall: IrCall): Boolean = irCall.symbol in arrayGetSymbols

  fun isArraySet(irCall: IrCall): Boolean = irCall.symbol in arraySetSymbols

  fun isArrayOfNull(irCall: IrCall): Boolean = irCall.symbol == irBuiltIns.arrayOfNulls

  fun isCheckNotNull(irCall: IrCall): Boolean = irCall.symbol == irBuiltIns.checkNotNullSymbol

  fun isNoWhenBranchMatchedException(irCall: IrCall): Boolean =
    irCall.symbol == irBuiltIns.noWhenBranchMatchedExceptionSymbol

  private val javaClassPropertyGetter by lazy { topLevelGetterSymbol(kotlinJvmFqn, "javaClass") }

  fun isJavaClassProperty(irCall: IrCall): Boolean = irCall.symbol == javaClassPropertyGetter

  private val kClassJavaPropertyGetter by lazy { topLevelGetterSymbol(kotlinJvmFqn, "java") }

  fun isKClassJavaProperty(irCall: IrCall): Boolean = irCall.symbol == kClassJavaPropertyGetter

  private val kClassJavaObjectTypePropertyGetter by lazy {
    topLevelGetterSymbol(kotlinJvmFqn, "javaObjectType")
  }

  fun isKClassJavaObjectTypeProperty(irCall: IrCall): Boolean =
    irCall.symbol == kClassJavaObjectTypePropertyGetter

  // Contains the set of all symbols corresponding to the different arrayOf functions.
  // e.g. [arrayOf, intArrayOf, booleanArrayOf...]
  private val arrayOfSymbols: Set<IrFunctionSymbol> by lazy {
    buildSet {
      add(irBuiltIns.arrayOf)
      for (primitive in irBuiltIns.primitiveTypesToPrimitiveArrays.keys) {
        add(topLevelFunctionSymbol(kotlinFqn, "${primitive.name.lowercase()}ArrayOf"))
      }
    }
  }

  fun isArrayOf(irCall: IrCall): Boolean = irCall.symbol in arrayOfSymbols

  private val emptyArraySymbol by lazy { topLevelFunctionSymbol(kotlinFqn, "emptyArray") }

  fun isEmptyArray(irCall: IrCall): Boolean = irCall.symbol == emptyArraySymbol

  private val isArrayOfSymbol by lazy { topLevelFunctionSymbol(kotlinJvmFqn, "isArrayOf") }

  fun isIsArrayOf(irCall: IrCall): Boolean = irCall.symbol == isArrayOfSymbol

  fun isArrayIterator(irCall: IrCall): Boolean = irCall.symbol in arrayIteratorSymbols

  fun isDataClassArrayMemberToString(irCall: IrCall): Boolean =
    irCall.symbol == irBuiltIns.dataClassArrayMemberToStringSymbol

  fun isDataClassArrayMemberHashCode(irCall: IrCall): Boolean =
    irCall.symbol == irBuiltIns.dataClassArrayMemberHashCodeSymbol

  fun isAnyToString(irCall: IrCall): Boolean = irCall.symbol == symbols.extensionToString

  fun isEqualsOperator(irCall: IrCall): Boolean = irCall.symbol == irBuiltIns.eqeqSymbol

  fun isReferenceEqualsOperator(irCall: IrCall): Boolean = irCall.symbol == irBuiltIns.eqeqeqSymbol

  private val ieee754EqualsSymbols: Set<IrFunctionSymbol> by lazy {
    irBuiltIns.ieee754equalsFunByOperandType.values.toSet()
  }

  fun isIeee754EqualsOperator(irCall: IrCall): Boolean = irCall.symbol in ieee754EqualsSymbols

  private val compareToSymbols: Set<IrFunctionSymbol> by lazy {
    binaryFunctionSymbols(StandardClassIds.primitiveTypes, "compareTo").toSet()
  }

  fun isCompareTo(irCall: IrCall): Boolean = irCall.symbol in compareToSymbols

  private val jsUtilsClassId = ClassId(FqName("javaemul.internal"), Name.identifier("JsUtils"))

  val jsUndefinedSymbol: IrSimpleFunctionSymbol by lazy {
    functionSymbol(jsUtilsClassId, "undefined")
  }

  fun isGetJsUndefinedCall(irCall: IrCall): Boolean = irCall.symbol == jsUndefinedSymbol

  val jsIsUndefinedFunctionSymbol: IrSimpleFunctionSymbol by lazy {
    functionSymbol(jsUtilsClassId, "isUndefined")
  }

  val jsCoerceToNullSymbol: IrSimpleFunctionSymbol by lazy {
    functionSymbol(jsUtilsClassId, "coerceToNull")
  }

  fun getPrefixOperator(symbol: IrFunctionSymbol): PrefixOperator? =
    prefixOperatorByIntrinsicSymbol[symbol]

  fun isPrefixOperation(irCall: IrCall): Boolean = irCall.symbol in prefixOperatorByIntrinsicSymbol

  fun getBinaryOperator(symbol: IrFunctionSymbol): BinaryOperator? =
    binaryOperatorByIntrinsicSymbol[symbol]

  fun isBinaryOperation(irCall: IrCall): Boolean = irCall.symbol in binaryOperatorByIntrinsicSymbol

  fun isRangeTo(irCall: IrCall): Boolean = irCall.symbol in rangeToSymbols

  fun isRangeUntil(irCall: IrCall): Boolean = irCall.symbol in rangeUntilSymbols

  private val enumEntriesSymbol by lazy {
    topLevelFunctionSymbol(FqName("kotlin.enums"), "enumEntriesIntrinsic")
  }

  fun isEnumEntries(irCall: IrCall): Boolean = irCall.symbol == enumEntriesSymbol

  private val enumGetEntriesName = Name.special("<get-entries>")

  /**
   * Returns whether the call is to the `entries` getter of an enum class. Symbol identity cannot be
   * used here: `entries` is not declared by `kotlin.Enum` but synthesized in every enum class.
   */
  fun isEnumGetEntries(irCall: IrCall): Boolean =
    with(irCall.symbol.owner) {
      name == enumGetEntriesName &&
        parentClassOrNull?.isEnumClass == true &&
        hasShape(dispatchReceiver = false, extensionReceiver = false, regularParameters = 0)
    }

  private val coroutineContextGetterSymbol: IrSimpleFunctionSymbol by lazy {
    topLevelGetterSymbol(StandardClassIds.BASE_COROUTINES_PACKAGE, "coroutineContext")
  }

  fun isCoroutineContextGetterCall(symbol: IrFunctionSymbol): Boolean =
    symbol == coroutineContextGetterSymbol

  val getContinuationSymbol: IrSimpleFunctionSymbol by lazy {
    topLevelFunctionSymbol(StandardClassIds.BASE_COROUTINES_INTRINSICS_PACKAGE, "getContinuation")
  }

  private val prefixOperatorByIntrinsicSymbol: Map<IrFunctionSymbol, PrefixOperator> by lazy {
    val numericClasses =
      listOf(
        StandardClassIds.Byte,
        StandardClassIds.Short,
        StandardClassIds.Int,
        StandardClassIds.Long,
        StandardClassIds.Float,
        StandardClassIds.Double,
      )
    val numericAndCharClasses = numericClasses + StandardClassIds.Char
    (mapPrefixOperation("inc", PrefixOperator.INCREMENT, numericAndCharClasses) +
        mapPrefixOperation("dec", PrefixOperator.DECREMENT, numericAndCharClasses) +
        mapPrefixOperation("unaryMinus", PrefixOperator.MINUS, numericClasses) +
        mapPrefixOperation("unaryPlus", PrefixOperator.PLUS, numericClasses) +
        mapPrefixOperation(
          "inv",
          PrefixOperator.COMPLEMENT,
          listOf(StandardClassIds.Int, StandardClassIds.Long),
        ) +
        listOf(irBuiltIns.booleanNotSymbol to PrefixOperator.NOT))
      .toMap()
  }

  private val binaryOperatorByIntrinsicSymbol: Map<IrFunctionSymbol, BinaryOperator> by lazy {
    (mapBinaryOperation("plus", BinaryOperator.PLUS) +
        mapBinaryOperation("minus", BinaryOperator.MINUS) +
        mapBinaryOperation("div", BinaryOperator.DIVIDE) +
        mapBinaryOperation("times", BinaryOperator.TIMES) +
        mapBinaryOperation("rem", BinaryOperator.REMAINDER) +
        mapBinaryOperation("and", BinaryOperator.BIT_AND) +
        mapBinaryOperation("or", BinaryOperator.BIT_OR) +
        mapBinaryOperation("xor", BinaryOperator.BIT_XOR) +
        mapBinaryOperation("shl", BinaryOperator.LEFT_SHIFT) +
        mapBinaryOperation("shr", BinaryOperator.RIGHT_SHIFT_SIGNED) +
        mapBinaryOperation("ushr", BinaryOperator.RIGHT_SHIFT_UNSIGNED) +
        mapComparisonBinaryOperation(irBuiltIns.lessFunByOperandType, BinaryOperator.LESS) +
        mapComparisonBinaryOperation(
          irBuiltIns.lessOrEqualFunByOperandType,
          BinaryOperator.LESS_EQUALS,
        ) +
        mapComparisonBinaryOperation(irBuiltIns.greaterFunByOperandType, BinaryOperator.GREATER) +
        mapComparisonBinaryOperation(
          irBuiltIns.greaterOrEqualFunByOperandType,
          BinaryOperator.GREATER_EQUALS,
        ) +
        listOf(
          symbols.extensionStringPlus to BinaryOperator.PLUS,
          symbols.memberStringPlus to BinaryOperator.PLUS,
        ))
      .toMap()
  }

  /**
   * Create a mapping between all definitions of a method that exists on primitives and the
   * equivalent binary operator in J2CL.
   */
  private fun mapBinaryOperation(
    methodName: String,
    binaryOperator: BinaryOperator,
  ): List<Pair<IrFunctionSymbol, BinaryOperator>> {
    val applicableClasses =
      mutableListOf(
        StandardClassIds.Char,
        StandardClassIds.Byte,
        StandardClassIds.Short,
        StandardClassIds.Int,
        StandardClassIds.Long,
      )
    if (binaryOperator.isBitwiseOperator) {
      applicableClasses.add(StandardClassIds.Boolean)
    } else if (!binaryOperator.isShiftOperator) {
      applicableClasses.addAll(listOf(StandardClassIds.Float, StandardClassIds.Double))
    }

    // The method we want to map is defined on each class of {@code applicableClasses} and they are
    // overloaded for accepting as single parameter each class of {@code applicableClasses}.
    // The code below creates the mappings for the methods and all possible overloads.
    return binaryFunctionSymbols(applicableClasses, methodName).map { it to binaryOperator }
  }

  /**
   * Returns the overloads of the member function [name] of the classes [classIds] that take a
   * single parameter whose type is also one of [classIds].
   */
  private fun binaryFunctionSymbols(
    classIds: Collection<ClassId>,
    name: String,
  ): List<IrSimpleFunctionSymbol> = classIds.flatMap { classId ->
    functionSymbols(classId, name).filter { it.singleParameterClassIdOrNull() in classIds }
  }

  /** Returns the class id of the type of the single regular parameter of the function, if any. */
  private fun IrSimpleFunctionSymbol.singleParameterClassIdOrNull(): ClassId? =
    owner.parameters
      .singleOrNull { it.kind == IrParameterKind.Regular }
      ?.type
      ?.classOrNull
      ?.owner
      ?.classId

  /**
   * Create a mapping between the parameterless member function [methodName] of each class of
   * [classIds] and the equivalent unary operator in J2CL.
   */
  private fun mapPrefixOperation(
    methodName: String,
    prefixOperator: PrefixOperator,
    classIds: List<ClassId>,
  ): List<Pair<IrFunctionSymbol, PrefixOperator>> = classIds.map { classId ->
    val symbol =
      functionSymbol(classId, methodName) { function ->
        function.parameters.none { it.kind == IrParameterKind.Regular }
      }
    symbol to prefixOperator
  }

  /**
   * Creates a mapping between each comparison intrinsic functions defined on Primitive classes and
   * the equivalent binary operator in J2CL
   */
  private fun mapComparisonBinaryOperation(
    intrinsicComparisonFunctionByPrimitiveClass: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>,
    binaryOperator: BinaryOperator,
  ) = intrinsicComparisonFunctionByPrimitiveClass.values.map { it to binaryOperator }

  private val rangeToSymbols by lazy { buildRangeOperatorSymbols("rangeTo") }
  private val rangeUntilSymbols by lazy { buildRangeOperatorSymbols("rangeUntil") }

  private fun buildRangeOperatorSymbols(methodName: String): Set<IrFunctionSymbol> =
    (binaryFunctionSymbols(listOf(StandardClassIds.Char), methodName) +
        binaryFunctionSymbols(
          listOf(
            StandardClassIds.Byte,
            StandardClassIds.Short,
            StandardClassIds.Int,
            StandardClassIds.Long,
          ),
          methodName,
        ))
      .toSet()
}
