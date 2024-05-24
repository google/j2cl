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

package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.transpiler.ast.BinaryOperator
import com.google.j2cl.transpiler.ast.PrefixOperator
import com.google.j2cl.transpiler.frontend.kotlin.ir.isArrayType
import com.google.j2cl.transpiler.frontend.kotlin.ir.nonNullFqn
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName

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
class IntrinsicMethods(val irBuiltIns: IrBuiltIns) {
  private val kotlinFqn = StandardNames.BUILT_INS_PACKAGE_FQ_NAME
  private val intFqn = StandardNames.FqNames._int.toSafe()
  private val arrayFqn = StandardNames.FqNames.array.toSafe()

  fun isNewArray(irConstructorCall: IrConstructorCall): Boolean =
    irConstructorCall.symbol.owner.parentClassOrNull?.defaultType?.isArrayType() ?: false

  fun isArraySize(irCall: IrCall): Boolean = irCall.isArrayMethodCall("<get-size>")

  fun isArrayGet(irCall: IrCall): Boolean = irCall.isArrayMethodCall("get")

  fun isArraySet(irCall: IrCall): Boolean = irCall.isArrayMethodCall("set")

  fun isArrayOfNull(irCall: IrCall): Boolean {
    return irCall.symbol.toKey() == Key(kotlinFqn, "arrayOfNulls", listOf(intFqn))
  }

  fun isCheckNotNull(irCall: IrCall): Boolean {
    return irCall.symbol.toKey() == irBuiltIns.checkNotNullSymbol.toKey()
  }

  fun isNoWhenBranchMatchedException(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == irBuiltIns.noWhenBranchMatchedExceptionSymbol.toKey()

  fun isJavaClassProperty(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == Key(FqName("kotlin.jvm"), "<get-javaClass>")

  fun isKClassJavaProperty(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == Key(FqName("kotlin.jvm"), "<get-java>")

  fun isKClassJavaObjectTypeProperty(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == Key(FqName("kotlin.jvm"), "<get-javaObjectType>")

  // Contains the set of all symbols corresponding to the different arrayOf functions.
  // e.g. [arrayOf, intArrayOf, booleanArrayOf...]
  private val arrayOfSymbolKeys =
    buildSet<Key> {
      add(Key(kotlinFqn, "arrayOf", listOf(arrayFqn)))
      irBuiltIns.primitiveTypesToPrimitiveArrays.forEach { (primitive, primitiveArray) ->
        add(
          Key(kotlinFqn, "${primitive.name.lowercase()}ArrayOf", listOf(primitiveArray.nonNullFqn))
        )
      }
    }

  fun isArrayOf(irCall: IrCall): Boolean {
    return irCall.symbol.toKey() in arrayOfSymbolKeys
  }

  fun isIsArrayOf(irCall: IrCall): Boolean {
    return irCall.symbol.toKey() == Key(FqName("kotlin.jvm"), "isArrayOf", emptyList())
  }

  fun isArrayIterator(irCall: IrCall): Boolean = irCall.isArrayMethodCall("iterator")

  fun isDataClassArrayMemberToString(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == irBuiltIns.dataClassArrayMemberToStringSymbol.toKey()

  fun isDataClassArrayMemberHashCode(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == irBuiltIns.dataClassArrayMemberHashCodeSymbol.toKey()

  fun isAnyToString(irCall: IrCall) = irCall.symbol.toKey() == irBuiltIns.extensionToString.toKey()

  fun isEqualsOperator(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == irBuiltIns.eqeqSymbol.toKey()

  fun isReferenceEqualsOperator(irCall: IrCall): Boolean =
    irCall.symbol.toKey() == irBuiltIns.eqeqeqSymbol.toKey()

  private val ieee754EqualsSymbolKeys =
    irBuiltIns.ieee754equalsFunByOperandType.values.map { it.toKey() }.toSet()

  fun isIeee754EqualsOperator(irCall: IrCall): Boolean =
    irCall.symbol.toKey() in ieee754EqualsSymbolKeys

  fun getPrefixOperator(symbol: IrFunctionSymbol): PrefixOperator? =
    prefixOperatorByIntrinsicSymbolKey[symbol.toKey()]

  fun isPrefixOperation(irCall: IrCall): Boolean =
    irCall.symbol.toKey() in prefixOperatorByIntrinsicSymbolKey

  fun getBinaryOperator(symbol: IrFunctionSymbol): BinaryOperator? =
    binaryOperatorByIntrinsicSymbolKey[symbol.toKey()]

  fun isBinaryOperation(irCall: IrCall): Boolean =
    irCall.symbol.toKey() in binaryOperatorByIntrinsicSymbolKey

  fun getRangeToConstructor(irCall: IrCall): IrConstructorSymbol {
    val fqName = irCall.type.classFqName!!
    val classSymbol = irBuiltIns.findClass(fqName.shortName(), fqName.parent())!!
    return classSymbol.constructors.single { it.owner.valueParameters.size == 2 }
  }

  fun isRangeTo(irCall: IrCall): Boolean = irCall.symbol.toKey() in rangeToCallByIntrinsicSymbolKey

  fun isRangeUntil(irCall: IrCall): Boolean =
    irCall.symbol.toKey() in rangeUntilCallByIntrinsicSymbolKey

  private val prefixOperatorByIntrinsicSymbolKey =
    (mapPrefixOperation("not", PrefixOperator.COMPLEMENT) +
        mapPrefixOperation("inc", PrefixOperator.INCREMENT) +
        mapPrefixOperation("dec", PrefixOperator.DECREMENT) +
        mapPrefixOperation("unaryMinus", PrefixOperator.MINUS) +
        mapPrefixOperation("unaryPlus", PrefixOperator.PLUS) +
        mapPrefixOperation("inv", PrefixOperator.COMPLEMENT) +
        listOf(irBuiltIns.booleanNotSymbol.toKey() to PrefixOperator.NOT))
      .toMap()

  private val binaryOperatorByIntrinsicSymbolKey =
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
          irBuiltIns.extensionStringPlus.toKey() to BinaryOperator.PLUS,
          irBuiltIns.memberStringPlus.toKey() to BinaryOperator.PLUS,
        ))
      .toMap()

  /**
   * Create a mapping between all definitions of a method that exists on primitives and the
   * equivalent binary operator in J2CL.
   */
  private fun mapBinaryOperation(
    methodName: String,
    binaryOperator: BinaryOperator,
  ): List<Pair<Key, BinaryOperator>> {
    val applicableClasses =
      mutableListOf(
        irBuiltIns.charClass,
        irBuiltIns.byteClass,
        irBuiltIns.shortClass,
        irBuiltIns.intClass,
        irBuiltIns.longClass,
      )
    if (binaryOperator.isBitwiseOperator) {
      applicableClasses.add(irBuiltIns.booleanClass)
    } else if (!binaryOperator.isShiftOperator) {
      applicableClasses.addAll(listOf(irBuiltIns.floatClass, irBuiltIns.doubleClass))
    }

    // The method we want to map is defined on each class of {@code applicableClasses} and they are
    // overloaded for accepting as single parameter each class of {@code applicableClasses}.
    // The code below creates the mappings for the methods and all possible overloads.
    return applicableClasses.flatMap { leftSide ->
      applicableClasses.map { rightSide ->
        Key(leftSide.nonNullFqn, methodName, listOf(rightSide.nonNullFqn)) to binaryOperator
      }
    }
  }

  /**
   * Create a mapping between all definitions of a method that exists on primitives and the
   * equivalent unary operator in J2CL.
   */
  private fun mapPrefixOperation(
    methodName: String,
    prefixOperator: PrefixOperator,
  ): List<Pair<Key, PrefixOperator>> {
    val applicableClasses =
      mutableListOf(
        irBuiltIns.charClass,
        irBuiltIns.byteClass,
        irBuiltIns.shortClass,
        irBuiltIns.intClass,
        irBuiltIns.longClass,
        irBuiltIns.floatClass,
        irBuiltIns.doubleClass,
      )

    // The method we want to map is defined on each class of {@code applicableClasses}, hence
    // create a map for each of the variants.
    return applicableClasses.map { operand ->
      Key(operand.nonNullFqn, methodName, listOf()) to prefixOperator
    }
  }

  /**
   * Creates a mapping between each comparison intrinsic functions defined on Primitive classes and
   * the equivalent binary operator in J2CL
   */
  private fun mapComparisonBinaryOperation(
    intrinsicComparisonFunctionByPrimitiveClass: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>,
    binaryOperator: BinaryOperator,
  ) = intrinsicComparisonFunctionByPrimitiveClass.map { it.value.toKey() to binaryOperator }

  private val rangeToCallByIntrinsicSymbolKey =
    buildRangeOperatorCallByIntrinsicSymbolKey("rangeTo")
  private val rangeUntilCallByIntrinsicSymbolKey =
    buildRangeOperatorCallByIntrinsicSymbolKey("rangeUntil")

  private fun buildRangeOperatorCallByIntrinsicSymbolKey(methodName: String) =
    buildSet() {
      addAll(
        mapRangeOperatorCall(listOf(irBuiltIns.charClass), listOf(irBuiltIns.charClass), methodName)
      )

      val numericRangeToTypes =
        listOf(
          irBuiltIns.byteClass,
          irBuiltIns.shortClass,
          irBuiltIns.intClass,
          irBuiltIns.longClass,
        )
      addAll(mapRangeOperatorCall(numericRangeToTypes, numericRangeToTypes, methodName))
    }

  /** Creates a mapping between the specified types for the `rangeTo` call. */
  private fun mapRangeOperatorCall(
    leftSide: List<IrClassifierSymbol>,
    rightSide: List<IrClassifierSymbol>,
    methodName: String,
  ): List<Key> =
    leftSide.flatMap { left ->
      rightSide.map { right -> Key(left.nonNullFqn, methodName, listOf(right.nonNullFqn)) }
    }

  private data class Key(
    val owner: FqName,
    val name: String,
    val valueParameterTypeFqNames: List<FqName?> = emptyList(),
  )

  private fun IrFunctionSymbol.toKey(): Key {
    val parent = owner.parent
    val parentFqn =
      when {
        // For top level members, we use the fqn of the package defined in the file without
        // including the file name so, we are consistent with kotlin method fqn used in imports.
        parent is IrClass && parent.isFileClass ->
          (parent.parent as IrPackageFragment).packageFqName
        // For instance method, use the fqn of the enclosing class.
        parent is IrClass -> parent.symbol.nonNullFqn
        // Some intrinsic methods are not defined in a file and their direct parent is the package
        // they belong to.
        parent is IrPackageFragment -> parent.packageFqName
        else -> throw IllegalStateException("Parent not supported $parent: ${parent.kotlinFqName}")
      }

    return toKey(parentFqn)
  }

  private fun IrFunctionSymbol.toKey(receiver: FqName): Key {
    return Key(receiver, owner.name.asString(), owner.valueParameters.map(::getParameterTypeFqName))
  }
}

private fun getParameterTypeFqName(irValueParameter: IrValueParameter): FqName =
  irValueParameter.type.fullyQualifiedName

private fun IrCall.isArrayMethodCall(methodName: String): Boolean {
  val callee = symbol.owner
  val calleeEnclosingType = callee.parentClassOrNull?.defaultType ?: return false

  return calleeEnclosingType.isArrayType() && callee.name.asString() == methodName
}

// TODO(dramaix): move shared extension functions to a file named IrUtils.kt
internal val IrType.fullyQualifiedName: FqName
  get() = checkNotNull(classifierOrNull?.nonNullFqn) { "Fqn not available for this type [$this]" }
