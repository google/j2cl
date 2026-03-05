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

import org.jetbrains.kotlin.backend.common.lower.AbstractPropertyReferenceLowering
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Transforms [IrRichPropertyReference] into instantiations of the corresponding internal
 * `KProperty` implementation classes from `kotlin.jvm.internal`.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class, ObsoleteDescriptorBasedAPI::class)
internal class J2clPropertyReferenceLowering(context: J2clBackendContext) :
  AbstractPropertyReferenceLowering<J2clBackendContext>(context) {

  override fun functionReferenceClass(arity: Int): IrClassSymbol {
    return context.irBuiltIns.functionN(arity).symbol
  }

  override fun IrBuilderWithScope.createKProperty(
    reference: IrRichPropertyReference,
    typeArguments: List<IrType>,
    getterReference: IrRichFunctionReference,
    setterReference: IrRichFunctionReference?,
  ): IrExpression {
    val arity = typeArguments.size - 1
    // The `arity` here corresponds to the number of receiver types in the `KProperty` interface:
    // -   `KProperty0<V>` has an arity of 0. This covers properties without a receiver (top-level)
    //     or those with a receiver already bound (`aReference::aProperty`).
    // -   `KProperty1<D, V>` has an arity of 1. This covers properties that take one receiver `D`
    //     as a parameter (`MyClass::aProperty`).
    //
    // We expect the arity to be at most 1 because `KProperty2<D1, D2, V>` (arity 2) is not
    // relevant for property references created via the `::` operator. `KProperty2` represents
    // properties requiring two receivers (e.g., extension properties defined in another class).
    // Such properties cannot be directly referenced using `::` in user code. Accessing them
    // would require reflection APIs, which are not supported by J2CL.
    check(arity <= 1)
    val constructor =
      loadPrimaryConstructorSymbol("kotlin.jvm.internal.MutableKProperty${arity}Impl")

    return irCallConstructor(constructor, typeArguments).apply {
      arguments[0] = getterReference
      arguments[1] = setterReference ?: irNull()
    }
  }

  override fun IrBuilderWithScope.createLocalKProperty(
    reference: IrRichPropertyReference,
    propertyName: String,
    propertyType: IrType,
    isMutable: Boolean,
  ): IrExpression {
    val propertySymbol = reference.reflectionTargetSymbol
    check(propertySymbol is IrLocalDelegatedPropertySymbol)

    val constructor = loadPrimaryConstructorSymbol("kotlin.jvm.internal.LocalVariableKPropertyImpl")
    return irCallConstructor(constructor, listOf(propertySymbol.owner.type))
  }

  private fun loadPrimaryConstructorSymbol(fqName: String): IrConstructorSymbol {
    val classId = ClassId.topLevel(FqName(fqName))
    val classSymbol =
      context.jvmBackendContext.irPluginContext!!.referenceClass(classId)
        ?: error("Could not find $classId")

    return classSymbol.owner.primaryConstructor?.symbol
      ?: error("Could not find primary constructor for $classId")
  }
}
