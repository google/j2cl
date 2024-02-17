/*
 * Copyright 2010-2019 JetBrains s.r.o.
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.asInlinable
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Rewrites `Array(size) { index -> value }` using type-specific initializer lambdas.
 *
 * This lowering will replace the initializer from being a Function1<Int, T> into a specific type
 * appropriate for the interface (ex. for IntArray it'll be an untemplated IntArrayInitializer).
 * This avoids the need for boxing and be directly translated into the J2CL AST.
 *
 * Since array initializers are inline, they can return out-of-scope. In these cases we perform a
 * fallback lowering wherein the entire initializer is inlined in a loop at the call-site.
 * Semantically it doesn't make sense for user-code to do this, so it should be incredibly rare that
 * this fallback operation is ever used.
 */
class ArrayConstructorLowering(private val context: JvmBackendContext) :
  BodyLoweringPass, IrElementTransformerVoidWithContext() {

  private var fallbackTransformer: ArrayConstructorTransformer? = null

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    fallbackTransformer = ArrayConstructorTransformer(context, container)
    irBody.transformChildrenVoid()
  }

  override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
    // Transform children first so that we operate post-fix.
    expression.transformChildrenVoid(this)

    val irConstructor = expression.symbol.owner
    val classConstructed = irConstructor.constructedClass.symbol

    if (
      (!classConstructed.isArrayClass && !classConstructed.isPrimitiveArrayClass) ||
        irConstructor.valueParameters.size != 2
    ) {
      return expression
    }

    val originalInitializer = expression.getValueArgument(1)!!

    // Since the array constructors are inline functions, the initializer lambda can return out of
    // context of the lambda (ex. return to the function it's being inlined into). In these cases
    // we must rewrite the call so that the lambda is inlined to the call-site so that the return
    // can be handled properly.
    if (originalInitializer is IrFunctionExpression && escapesScope(originalInitializer.function)) {
      return fallbackTransformer!!.visitConstructorCall(expression)
    }

    val originalInitializerType = originalInitializer.type as IrSimpleType

    if (originalInitializer.isEligibleForTypeReplacement) {
      // If the initializer is an inline lambda or function reference _and_ it accepts a
      // non-nullable int param, we can simply swap out the type of the functional interface it's
      // implementing.
      val typeArguments =
        // Primitive array initializers are not templated at all therefore we can drop these.
        if (classConstructed.isPrimitiveArrayClass) emptyList()
        // Non-primitive array initializers only need the value type as it's well-defined that we'll
        // always be passing in an int index.
        else listOf(originalInitializerType.arguments[1])

      val replacementType =
        IrSimpleTypeImpl(
          replacementInitializerFor(classConstructed),
          originalInitializerType.isNullable(),
          typeArguments,
          originalInitializerType.annotations,
        )

      originalInitializer.type = replacementType
    } else {
      // For all other cases we need to pass the initializer to an adapter that can resolve the
      // boxing difference between the `Function1` functional interface and the replacement
      // functional interface.
      val wrapperCall =
        IrCallImpl.fromSymbolOwner(
            startOffset = originalInitializer.startOffset,
            endOffset = originalInitializer.endOffset,
            adapterInitializerFor(classConstructed)
          )
          .also {
            if (!classConstructed.isPrimitiveArrayClass) {
              it.putTypeArgument(0, originalInitializerType.arguments[1].typeOrNull)
            }
            it.putValueArgument(0, originalInitializer)
          }
      expression.putValueArgument(1, wrapperCall)
    }
    return expression
  }

  private fun replacementInitializerFor(arrayClass: IrClassSymbol): IrClassSymbol {
    val name =
      when (arrayClass) {
        context.irBuiltIns.booleanArray -> Name.identifier("BooleanArrayInitializer")
        context.irBuiltIns.intArray -> Name.identifier("IntArrayInitializer")
        context.irBuiltIns.longArray -> Name.identifier("LongArrayInitializer")
        context.irBuiltIns.shortArray -> Name.identifier("ShortArrayInitializer")
        context.irBuiltIns.byteArray -> Name.identifier("ByteArrayInitializer")
        context.irBuiltIns.charArray -> Name.identifier("CharArrayInitializer")
        context.irBuiltIns.doubleArray -> Name.identifier("DoubleArrayInitializer")
        context.irBuiltIns.floatArray -> Name.identifier("FloatArrayInitializer")
        else -> Name.identifier("ArrayInitializer")
      }
    return checkNotNull(context.irBuiltIns.findClass(name, FqName("kotlin.jvm.internal")))
  }

  private fun adapterInitializerFor(arrayClass: IrClassSymbol): IrSimpleFunctionSymbol {
    val name =
      when (arrayClass) {
        context.irBuiltIns.booleanArray -> Name.identifier("toBooleanArrayInitializer")
        context.irBuiltIns.intArray -> Name.identifier("toIntArrayInitializer")
        context.irBuiltIns.longArray -> Name.identifier("toLongArrayInitializer")
        context.irBuiltIns.shortArray -> Name.identifier("toShortArrayInitializer")
        context.irBuiltIns.byteArray -> Name.identifier("toByteArrayInitializer")
        context.irBuiltIns.charArray -> Name.identifier("toCharArrayInitializer")
        context.irBuiltIns.doubleArray -> Name.identifier("toDoubleArrayInitializer")
        context.irBuiltIns.floatArray -> Name.identifier("toFloatArrayInitializer")
        else -> Name.identifier("toArrayInitializer")
      }
    return context.irBuiltIns.findFunctions(name, FqName("kotlin.jvm.internal")).single()
  }

  private val IrClassSymbol.isArrayClass: Boolean
    get() = this == context.irBuiltIns.arrayClass

  private val IrClassSymbol.isPrimitiveArrayClass: Boolean
    get() = this in context.irBuiltIns.primitiveArraysToPrimitiveTypes

  private val IrExpression.isEligibleForTypeReplacement: Boolean
    get() {
      // To be eligible for direct type replacement:
      //   1. It must be a IrFunctionExpression
      //   2. The function must take a single non-vararg value parameter
      //   3. The first type argument must be a non-nullable Int.
      // TODO(b/286111335): IrFunctionReference should also be eligible for replacement.
      if (this !is IrFunctionExpression) return false
      val valueParameters =
        (this as? IrFunctionExpression)?.function?.valueParameters
          ?: (this as IrFunctionReference).symbol.owner.valueParameters
      if (valueParameters.size != 1 || valueParameters[0].isVararg) return false
      return (type as IrSimpleType).arguments.getOrNull(0)?.typeOrNull ==
        context.irBuiltIns.intType.makeNotNull()
    }
}

private fun escapesScope(irFunction: IrFunction): Boolean {
  var escapesScope = false
  irFunction.acceptChildrenVoid(
    object : IrElementVisitorVoid {
      override fun visitElement(element: IrElement) {
        // Stop visiting if we've already found an escape.
        if (escapesScope) return
        element.acceptChildrenVoid(this)
      }

      override fun visitReturn(expression: IrReturn) {
        if (expression.returnTargetSymbol != irFunction.symbol) {
          escapesScope = true
        }
      }
    }
  )
  return escapesScope
}

/**
 * Rewrites array constructor call by inlining the initializer function in a loop at the call-site.
 *
 * Copied from org.jetbrains.kotlin.backend.common.lower.ArrayConstructorTransformer
 */
private class ArrayConstructorTransformer(
  val context: CommonBackendContext,
  val container: IrSymbolOwner
) : IrElementTransformerVoidWithContext() {

  // Array(size, init) -> Array(size)
  companion object {
    internal fun arrayInlineToSizeConstructor(
      context: CommonBackendContext,
      irConstructor: IrConstructor
    ): IrFunctionSymbol? {
      val clazz = irConstructor.constructedClass.symbol
      return when {
        irConstructor.valueParameters.size != 2 -> null
        clazz == context.irBuiltIns.arrayClass ->
          context.ir.symbols
            .arrayOfNulls // Array<T> has no unary constructor: it can only exist for Array<T?>
        context.irBuiltIns.primitiveArraysToPrimitiveTypes.contains(clazz) ->
          clazz.constructors.single { it.owner.valueParameters.size == 1 }
        else -> null
      }
    }
  }

  override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
    val sizeConstructor =
      arrayInlineToSizeConstructor(context, expression.symbol.owner)
        ?: return super.visitConstructorCall(expression)
    // inline fun <reified T> Array(size: Int, invokable: (Int) -> T): Array<T> {
    //     val result = arrayOfNulls<T>(size)
    //     for (i in 0 until size) {
    //         result[i] = invokable(i)
    //     }
    //     return result as Array<T>
    // }
    // (and similar for primitive arrays)
    val size = expression.getValueArgument(0)!!.transform(this, null)
    val invokable = expression.getValueArgument(1)!!.transform(this, null)
    if (invokable.type.isNothing()) {
      // Expressions of type 'Nothing' don't terminate.
      return invokable
    }
    val scope = (currentScope ?: createScope(container)).scope
    return context.createIrBuilder(scope.scopeOwnerSymbol).irBlock(
      expression.startOffset,
      expression.endOffset
    ) {
      val index = createTmpVariable(irInt(0), isMutable = true)
      val sizeVar = createTmpVariable(size)
      val result =
        createTmpVariable(
          irCall(sizeConstructor, expression.type).apply {
            copyTypeArgumentsFrom(expression)
            putValueArgument(0, irGet(sizeVar))
          }
        )

      val generator = invokable.asInlinable(this)
      +irWhile().apply {
        condition =
          irCall(context.irBuiltIns.lessFunByOperandType[index.type.classifierOrFail]!!).apply {
            putValueArgument(0, irGet(index))
            putValueArgument(1, irGet(sizeVar))
          }
        body = irBlock {
          val tempIndex = createTmpVariable(irGet(index))
          +irCall(
              result.type.getClass()!!.functions.single { it.name == OperatorNameConventions.SET }
            )
            .apply {
              dispatchReceiver = irGet(result)
              putValueArgument(0, irGet(tempIndex))
              val inlined =
                generator
                  .inline(parent, listOf(tempIndex))
                  .patchDeclarationParents(scope.getLocalDeclarationParent())
              putValueArgument(1, inlined)
            }
          val inc =
            index.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INC }
          +irSet(
            index.symbol,
            irCallOp(inc.symbol, index.type, irGet(index)),
            origin = IrStatementOrigin.PREFIX_INCR
          )
        }
      }
      +irGet(result)
    }
  }
}
