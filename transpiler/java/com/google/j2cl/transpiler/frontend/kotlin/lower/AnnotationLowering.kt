/*
 * Copyright 2025 Google Inc.
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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isKClass
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Rewrite annotation elements to match the java-like format:
 * - The annotation elements are modeled as properties with getter in the Kotlin IR. We transform
 *   them to normal functions.
 * - Remove any constructors of the annotation class.
 * - Make sure that any reference to `kotlin.KClass` is changed to `java.lang.Class`.
 * - Rewrite calls to annotation elements to refer to the java-like annotation elements that will be
 *   present at runtime.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class AnnotationLowering(private val context: JvmBackendContext) :
  FileLoweringPass, IrVisitorVoid() {
  override fun lower(irFile: IrFile) {
    irFile.acceptVoid(this)
  }

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }

  override fun visitClass(irClass: IrClass) {
    visitElement(irClass)

    if (!irClass.isAnnotationClass) {
      return
    }

    // Annotations are transpiled as interfaces to match Kotlin/JVM. Removes the constructor.
    irClass.declarations.removeIf { it is IrConstructor }

    // Rewrite the declaration of the annotation elements in the java-like format to match what
    // Kotlin/JVM produce in the bytecode.
    irClass.declarations.replaceAll {
      if (it.isAnnotationElement()) {
        createJavaLikeAnnotationElementFunction(it as IrSimpleFunction)
      } else {
        it
      }
    }
  }

  override fun visitCall(expression: IrCall) {
    visitElement(expression)

    val callee = expression.symbol.owner

    if (callee.isAnnotationElement()) {
      // Target the java-like annotation element that will be present at runtime.
      expression.symbol = createJavaLikeAnnotationElementFunction(callee).symbol
    }
  }

  /**
   * Our type model expect the annotation elements to be defined as interface methods but Kotlin
   * annotation elements are modeled as property getter. Transform the getter methods to normal
   * functions with their name set to the property name. Change `kotlin.KClass` reference in the
   * type of the element to `java.lang.Class`.
   */
  private fun createJavaLikeAnnotationElementFunction(
    kotlinElementAnnotationFunction: IrSimpleFunction
  ): IrSimpleFunction {
    return context.irFactory
      .buildFun {
        updateFrom(kotlinElementAnnotationFunction)
        name = kotlinElementAnnotationFunction.correspondingPropertySymbol!!.owner.name
        returnType = kotlinElementAnnotationFunction.returnType.kClassToJClassIfNeeded()
        modality = Modality.ABSTRACT
      }
      .apply {
        parent = kotlinElementAnnotationFunction.parent
        parameters = listOf(kotlinElementAnnotationFunction.dispatchReceiverParameter!!)
      }
  }

  private fun IrType.kClassToJClassIfNeeded(): IrType =
    when {
      this.isKClass() -> {
        val argument = (this as IrSimpleType).arguments.single()
        if (argument.typeOrNull == null) {
          context.symbols.javaLangClass.starProjectedType
        } else {
          context.symbols.javaLangClass.typeWith(argument.typeOrFail.kClassToJClassIfNeeded())
        }
      }
      this is IrSimpleType && isArray() && arguments.single().typeOrNull != null ->
        context.symbols.array.typeWith(arguments.single().typeOrFail.kClassToJClassIfNeeded())
      else -> this
    }
}

// TODO(dramaix): check how elements of Java annotations are represented in the IR.
private fun IrDeclaration.isAnnotationElement(): Boolean =
  this is IrSimpleFunction &&
    parent is IrClass &&
    parentAsClass.isAnnotationClass &&
    origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
