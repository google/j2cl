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

import com.google.j2cl.transpiler.frontend.kotlin.ir.fromQualifiedBinaryName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe

internal class BuiltinsResolver(
  private val pluginContext: IrPluginContext,
  private val jvmBackendContext: JvmBackendContext,
) {
  fun resolveFunctionSymbol(irFunctionSymbol: IrFunctionSymbol): IrFunctionSymbol {
    val resolvedClass =
      irFunctionSymbol.owner.parentClassOrNull?.symbol?.let(this::resolveClass)
        ?: return irFunctionSymbol

    val functionJvmSignature = irFunctionSymbol.getJvmSignature(jvmBackendContext)
    val functions =
      resolvedClass.functions
        .filter {
          !it.owner.isFakeOverride && functionJvmSignature == it.getJvmSignature(jvmBackendContext)
        }
        .toList()

    return when {
      functions.isEmpty() -> irFunctionSymbol
      functions.size == 1 -> functions.single()
      else ->
        throw IllegalStateException(
          "Found multiple matching functions for ${irFunctionSymbol.owner.parentClassOrNull!!.name}.${irFunctionSymbol.owner.name}"
        )
    }
  }

  private fun IrFunctionSymbol.getJvmSignature(jvmBackendContext: JvmBackendContext): String =
    jvmBackendContext.defaultMethodSignatureMapper.mapAsmMethod(owner).toString()

  fun resolveFieldSymbol(irFieldSymbol: IrFieldSymbol): IrFieldSymbol {
    val resolvedClass =
      irFieldSymbol.owner.parentClassOrNull?.symbol?.let(this::resolveClass) ?: return irFieldSymbol
    val fields = resolvedClass.fields.filter { it.owner.name == irFieldSymbol.owner.name }.toList()
    return when {
      fields.isEmpty() -> irFieldSymbol
      else -> fields.single()
    }
  }

  fun resolveClass(irClassSymbol: IrClassSymbol): IrClassSymbol? {
    val fqName = irClassSymbol.owner.kotlinFqName.toUnsafe()
    val mappedClassId =
      mapToIntrinsicImplementation(fqName)
        ?: JavaToKotlinClassMap.mapKotlinToJava(fqName)
        ?: return null
    val resolvedClass = pluginContext.referenceClass(mappedClassId) ?: return null
    check(resolvedClass.isBound) {
      "Resolved class to unbound symbol, originally: ${irClassSymbol.owner.render()}"
    }
    return resolvedClass
  }
}

private val intrinsicImplementations = buildMap {
  put("kotlin.Nothing", ClassId.fromQualifiedBinaryName("kotlin.jvm.internal.NothingStub"))

  // TODO(b/258286507): Add support for suspend function with arity > 22
  for (i in 0 until BuiltInFunctionArity.BIG_ARITY) {
    // `kotlin.coroutines.SuspendFunction{N}` are fictitious interfaces used by the Kotlin compiler
    // to type a suspend lambda. In J2CL, we need to convert to physical interfaces in order to make
    // our lambda machinery working.
    put(
      "kotlin.coroutines.SuspendFunction$i",
      ClassId.fromQualifiedBinaryName("kotlin.internal.j2cl.SuspendFunction$i"),
    )
  }
}

private fun mapToIntrinsicImplementation(kotlinFqName: FqNameUnsafe): ClassId? =
  intrinsicImplementations[kotlinFqName.asString()]
