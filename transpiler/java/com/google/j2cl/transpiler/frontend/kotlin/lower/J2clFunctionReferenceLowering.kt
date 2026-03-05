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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.isKFunctionOrKSuspendFunction
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Lowers [IrRichFunctionReference] to simplify their conversion to FunctionExpression.
 *
 * An [IrRichFunctionReference] is an IR node that unifies various function reference-like
 * constructs, including lambdas, anonymous functions, regular function references (`::foo`),
 * adapted function references, and SAM conversions. It can be conceptualized as an anonymous object
 * that implements a functional interface type.
 *
 * The structure of [IrRichFunctionReference] includes:
 * - `invokeFunction`: An [IrSimpleFunction] containing the implementation body of the lambda or
 *   referenced function.
 * - `boundValues`: A list of expressions captured by the reference at creation time. For a bound
 *   reference like `receiver::foo`, `receiver` is a bound value. When such a reference is invoked,
 *   bound values are passed as the initial arguments to `invokeFunction`.
 *
 * This lowering performs two main transformations:
 * 1. Bound Value Transformation: When an [IrRichFunctionReference] contains `boundValues`, this
 *    lowering creates a block expression that evaluates the bound values into temporary variables.
 *    Inside this block, a new `invokeFunction` is defined where the body is copied from the
 *    original `invokeFunction`. This new `invokeFunction` only accepts parameters for the unbound
 *    arguments. References to the bound parameters in the original body are replaced by references
 *    to the temporary variables, and references to the unbound parameters are replaced by
 *    references to the new function's parameters.
 *
 *    For example, for `receiver::foo` where `foo` is `fun Foo.foo(i: Int)`:
 *
 *    Before transformation:
 *
 *    IrRichFunctionReference:
 *         - boundValues: [receiver]
 *         - invokeFunction: `fun foo(receiver: Foo, i: Int) { receiver.foo(i) }`
 *
 *   After transformation:
 *
 *   IrBlock:
 *     - val tmp_bound_0 = receiver
 *     - IrRichFunctionReference:
 *         - boundValues: []
 *         - invokeFunction: `fun (p0: Int) { tmp_bound_0.foo(p0) }`
 * 2. `KFunctionN` to `FunctionN` Conversion: If the type of the [IrRichFunctionReference] is
 *    `KFunctionN` or `KSuspendFunctionN` (which are reflection-enabled types and represented by
 *    sybthetic interfaces), it is converted to the corresponding `FunctionN` or `SuspendFunctionN`
 *    type that are real functional interfaces.
 */
internal class J2clFunctionReferenceLowering(private val context: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoid() {

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(this)
  }

  override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
    expression.transformChildrenVoid(this)

    if (expression.boundValues.isEmpty()) {
      return convertKFunctionNToFunctionN(expression)
    }

    val originalInvokeFunction = expression.invokeFunction
    val boundValues = expression.boundValues

    val builder = context.createJvmIrBuilder(originalInvokeFunction.symbol)

    return builder.irBlock {
      // 1. Create temporary variables for specified bound values.
      val boundVariables =
        boundValues.mapIndexed { index, value ->
          // Note: irTemporary creates the variable, adds it to the builder and returns the
          // variable.
          irTemporary(value, nameHint = "bound$index")
        }

      // 2. Create the new invoke function that takes only the unbound parameters.
      val newInvokeFunction =
        context.irFactory
          .buildFun {
            name = SpecialNames.ANONYMOUS
            returnType = originalInvokeFunction.returnType
            updateFrom(originalInvokeFunction)
          }
          .apply {
            parent = originalInvokeFunction.parent
            // Add parameters for the unbound parameters.
            originalInvokeFunction.nonDispatchParameters.drop(boundValues.size).forEach { p ->
              val unused = addValueParameter {
                name = p.name
                type = p.type
              }
            }
          }

      // 3. Copy the body of the original invoke function into the new invoke function and update
      // the parameter references.
      val valueParameterMap = mutableMapOf<IrValueParameter, IrValueDeclaration>()

      originalInvokeFunction.nonDispatchParameters.forEachIndexed { index, param ->
        if (index < boundValues.size) {
          // Map bound parameters to temporary variables
          valueParameterMap[param] = boundVariables[index]
        } else {
          // Map unbound parameters to new function parameters
          valueParameterMap[param] = newInvokeFunction.parameters[index - boundValues.size]
        }
      }

      newInvokeFunction.body =
        originalInvokeFunction.body?.let { originalBody ->
          originalBody
            .transform(
              object : VariableRemapper(valueParameterMap) {
                override fun visitReturn(expression: IrReturn): IrExpression {
                  expression.transformChildrenVoid(this)
                  if (expression.returnTargetSymbol == originalInvokeFunction.symbol) {
                    // Update return expressions to return from the new invoke function.
                    return IrReturnImpl(
                      expression.startOffset,
                      expression.endOffset,
                      expression.type,
                      newInvokeFunction.symbol,
                      expression.value,
                    )
                  }
                  return expression
                }
              },
              null,
            )
            .apply { patchDeclarationParents(newInvokeFunction) }
        }

      // 4. Create the new RichFunctionReference that have no boundValues and use the new
      // invoke function.
      +convertKFunctionNToFunctionN(
        IrRichFunctionReferenceImpl(
          expression.startOffset,
          expression.endOffset,
          expression.type,
          expression.reflectionTargetSymbol,
          expression.overriddenFunctionSymbol,
          newInvokeFunction,
          expression.origin,
          expression.hasUnitConversion,
          expression.hasSuspendConversion,
          expression.hasVarargConversion,
          expression.isRestrictedSuspension,
        )
      )
    }
  }

  private fun convertKFunctionNToFunctionN(
    originalRichFunctionReference: IrRichFunctionReference
  ): IrRichFunctionReference {

    val expressionType = originalRichFunctionReference.type

    if (!expressionType.isKFunctionOrKSuspendFunction()) {
      return originalRichFunctionReference
    }

    // If it is a KFunction/KSuspendFunction interface, we need to convert it to a
    // FunctionN/SuspendFunctionN interface that are considered as fun interface by J2CL.`
    val kFunctionNType = expressionType as IrSimpleType
    val arity = kFunctionNType.arguments.size - 1
    val functionNClass =
      if (kFunctionNType.isKSuspendFunction()) {
        context.symbols.suspendFunctionN(arity)
      } else {
        context.symbols.functionN(arity)
      }
    val newExpressionType = functionNClass.typeWithArguments(kFunctionNType.arguments)

    return IrRichFunctionReferenceImpl(
      originalRichFunctionReference.startOffset,
      originalRichFunctionReference.endOffset,
      newExpressionType,
      originalRichFunctionReference.reflectionTargetSymbol,
      originalRichFunctionReference.overriddenFunctionSymbol,
      originalRichFunctionReference.invokeFunction,
      originalRichFunctionReference.origin,
      originalRichFunctionReference.hasUnitConversion,
      originalRichFunctionReference.hasSuspendConversion,
      originalRichFunctionReference.hasVarargConversion,
      originalRichFunctionReference.isRestrictedSuspension,
    )
  }
}
