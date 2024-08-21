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

import com.google.j2cl.transpiler.frontend.kotlin.ir.fqnOrFail
import com.google.j2cl.transpiler.frontend.kotlin.ir.fromQualifiedBinaryName
import com.google.j2cl.transpiler.frontend.kotlin.ir.isArrayType
import com.google.j2cl.transpiler.frontend.kotlin.ir.javaName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind.Function
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind.SuspendFunction
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getArrayElementType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe

internal class BuiltinsResolver(
  private val pluginContext: IrPluginContext,
  private val jvmbackendContext: JvmBackendContext,
) {
  fun resolveFunctionSymbol(irFunctionSymbol: IrFunctionSymbol): IrFunctionSymbol {
    val resolvedClass =
      irFunctionSymbol.owner.parentClassOrNull?.symbol?.let(this::resolveClass)
        ?: return irFunctionSymbol

    val functionJvmSignature = irFunctionSymbol.getJvmSignature(jvmbackendContext)
    val functions =
      resolvedClass.functions
        .filterNot { it.owner.isFakeOverride }
        .filter { javaFunction ->
          functionJvmSignature == javaFunction.getJvmSignature(jvmbackendContext)
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

  private fun IrFunctionSymbol.getJvmSignature(jvmbackendContext: JvmBackendContext): String {
    // Some builtin functions are mapped to jvm methods with a different name. Use the jvm name to
    // match the function.
    val functionName = owner.javaName(jvmbackendContext) ?: owner.name
    val parameterTypeSignatures =
      owner.valueParameters.map { it.type.getJvmTypeSignature() }.joinToString(separator = "")
    return "$functionName(${parameterTypeSignatures})${owner.returnType.getJvmTypeSignature()}"
  }

  private fun IrType.getJvmTypeSignature(): String =
    when {
      isArrayType() -> "[${getArrayElementType(pluginContext.irBuiltIns).getJvmTypeSignature()}"
      // Non-nullable kotlin primitives can match a java primitive or its boxed type. To simplify
      // our matching, we consider all primitive type being nullable.
      isPrimitiveType(nullable = false) -> makeNullable().getJvmTypeSignature()
      else -> {
        // `erasedUpperBound` returns the class of the upper bound if it's a type parameter,
        // otherwise the class of the type.
        val clazz = erasedUpperBound
        val clazzSymbol = resolveClass(clazz.symbol) ?: clazz.symbol
        "L${clazzSymbol.fqnOrFail};"
      }
    }

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
      mapToIntrinsicImplementation(fqName) ?: mapKotlinToJava(fqName) ?: return null
    val resolvedClass = pluginContext.referenceClass(mappedClassId) ?: return null
    check(resolvedClass.isBound) {
      "Resolved class to unbound symbol, originally: ${irClassSymbol.owner.render()}"
    }
    return resolvedClass
  }
}

private val suspendedFunctionFqnPrefix =
  "${SuspendFunction.packageFqName}.${SuspendFunction.classNamePrefix}"
private val functionFqnNamePrefix = "${Function.packageFqName}.${Function.classNamePrefix}"

private fun mapKotlinToJava(kotlinFqName: FqNameUnsafe): ClassId? {
  var mappingFqName = kotlinFqName

  // TODO(b/245558138): suspended functions are normally converted to a function type with an extra
  //  parameter that can be used to resume the coroutine with the return value (or exception if an
  //  error occurs).
  //  e.g.: kotlin.coroutines.SuspendFunction3 is mapped to kotlin.jvm.functions.Function4
  //  As we do not support coroutine right now, we are missing this extra parameter and
  //  the transpilation fails. For now, we will just consider suspended functions as normal
  //  functions.
  if (kotlinFqName.asString().startsWith(suspendedFunctionFqnPrefix)) {
    mappingFqName =
      FqNameUnsafe(
        kotlinFqName.asString().replace(suspendedFunctionFqnPrefix, functionFqnNamePrefix)
      )
  }
  return JavaToKotlinClassMap.mapKotlinToJava(mappingFqName)
}

private val intrinsicImplementations =
  mapOf("kotlin.Nothing" to ClassId.fromQualifiedBinaryName("kotlin.jvm.internal.NothingStub"))

private fun mapToIntrinsicImplementation(kotlinFqName: FqNameUnsafe): ClassId? =
  intrinsicImplementations[kotlinFqName.asString()]
