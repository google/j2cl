/*
 * Copyright 2024 Google Inc.
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
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.ir

import java.lang.IllegalArgumentException
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.IdSignature.AccessorSignature
import org.jetbrains.kotlin.ir.util.IdSignature.CommonSignature
import org.jetbrains.kotlin.ir.util.IdSignature.CompositeSignature
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * An `IrProvider` that loads IR nodes based on their public signature using the `IrPluginContext`.
 *
 * This provider is essential for IR deserialization with the K2 compiler. During deserialization,
 * unbound symbols are generated for references to external declarations that haven't been loaded
 * yet.
 *
 * In the K1 compiler, an `IrProvider` relying on the descriptor API is used to load or create IR
 * nodes for these symbols. However, the descriptor API is no longer available in K2. This provider
 * addresses this by utilizing the public signature of the unbound symbol and the IR plugin API to
 * resolve the corresponding IR node.
 *
 * Please refer to the [IdSignature documentation](http://shortn/_M5TtLgLERk) for a better
 * understanding of the logic of this class.
 */
class IrProviderFromPublicSignature(val pluginContext: IrPluginContext) : IrProvider {

  val irSignaturer = PublicIdSignatureComputer(JvmIrMangler)

  @Suppress("UNCHECKED_CAST")
  override fun getDeclaration(symbol: IrSymbol): IrDeclaration {
    // this cast is safe because all symbols are IrBindableSymbol.
    val bindableSymbol = symbol as IrBindableSymbol<*, IrDeclaration>

    if (bindableSymbol.isBound) {
      return bindableSymbol.owner
    }

    // After this check, we know a public signature is available for the symbol.
    if (bindableSymbol.signature == null || !bindableSymbol.signature!!.isPubliclyVisible) {
      throw IllegalArgumentException("This IrProvider does not support non public symbols $symbol")
    }

    bindableSymbol.bind(symbol.resolveOwner())

    return bindableSymbol.owner
  }

  private fun IrSymbol.resolveOwner(): IrDeclaration =
    when (this) {
      is IrClassSymbol -> pluginContext.referenceClass(classId)!!.owner
      is IrTypeAliasSymbol -> pluginContext.referenceTypeAlias(classId)!!.owner
      is IrConstructorSymbol ->
        pluginContext
          .referenceConstructors(enclosingClassId)
          .matchingDeclarationSignature(publicSignature)
      is IrSimpleFunctionSymbol -> {
        val signature = this.signature

        if (signature is AccessorSignature) {
          // Property accessor function. We need to look for the associated property and bind the
          // right accessor function.
          val propertySymbol =
            pluginContext
              .referenceProperties(signature.callableId)
              .matchingDeclarationSignature(signature.propertySignature.asPublic()!!)
          if (signature.isGetter) propertySymbol.getter!! else propertySymbol.setter!!
        } else {
          // Simple function.
          if (isStatic) {
            // There is no way to directly lookup for a java static function. We will look for
            // the enclosing class and then look up for the function within the class.
            pluginContext
              .referenceClass(enclosingClassId)
              .findDeclarationWithSignature<IrFunction>(publicSignature)
          } else {
            pluginContext
              .referenceFunctions(publicSignature.callableId)
              .matchingDeclarationSignature(publicSignature)
          }
        }
      }
      is IrFieldSymbol ->
        // We got IrFieldSymbol when an inline function refers to a java field (ex: The stdlib
        // `println` is an inline function to refers to `System.out` field). Unfortunately, the
        // `pluginContext.referenceProperties()` works only properties defined in Kotlin. We work
        // around this by looking at the declared properties of the enclosing class.
        pluginContext
          .referenceClass(enclosingClassId)
          .findDeclarationWithSignature<IrProperty>(publicSignature)
          .backingField!!
      is IrEnumEntrySymbol ->
        pluginContext
          .referenceClass(enclosingClassId)
          .findDeclarationWithSignature<IrEnumEntry>(publicSignature)
      is IrPropertySymbol ->
        pluginContext
          .referenceProperties(publicSignature.callableId)
          .matchingDeclarationSignature(publicSignature)
      else -> throw IllegalArgumentException("Unsupported symbol $this")
    }

  private inline fun <reified T : IrDeclaration> IrClassSymbol?.findDeclarationWithSignature(
    signature: CommonSignature
  ): T = this!!.owner.declarations.filterIsInstance<T>().matchingSignature(signature)

  private fun <T : IrDeclaration> Collection<T>.matchingSignature(signature: CommonSignature): T =
    single {
      val declarationSignature = irSignaturer.computeSignature(it).asPublic()!!
      // When matching declarations that can be inherited, we need to use the id of the signature.
      // This is because the symbol signature for a call on a child class to an inherited function
      // differs from the signature of the function declared in the parent class, even though they
      // represent the same underlying declaration. Using the id ensures consistent matching
      // across inheritance hierarchies.
      if (signature.id != null) {
        signature.id == declarationSignature.id
      } else {
        // Signature of declaration that cannot be inherited (like enum entries) does not have id.
        signature == declarationSignature
      }
    }

  private fun <T : IrDeclaration> Collection<IrBindableSymbol<*, T>>.matchingDeclarationSignature(
    signature: CommonSignature
  ): T = map { it.owner }.matchingSignature(signature)
}

private val IrSimpleFunctionSymbol.isStatic: Boolean
  get() = publicSignature.description?.contains("#static(") == true

private val AccessorSignature.isGetter: Boolean
  get() = accessorSignature.shortName.startsWith("<get")

private val AccessorSignature.callableId: CallableId
  get() = propertySignature.asPublic()!!.callableId

private val CommonSignature.callableId: CallableId
  get() {
    val callableName = Name.guessByFirstCharacter(shortName)
    val enclosingClassFqn =
      if (nameSegments.size > 1) {
        val enclosingClassSegments = nameSegments.subList(0, nameSegments.size - 1)
        FqName.fromSegments(enclosingClassSegments)
      } else {
        null
      }
    return CallableId(packageFqName(), enclosingClassFqn, callableName)
  }

private val IrSymbol.classId: ClassId
  get() = ClassId(publicSignature.packageFqName(), FqName(publicSignature.declarationFqName), false)

private val IrSymbol.enclosingClassId: ClassId
  get() =
    ClassId(publicSignature.packageFqName(), Name.identifier(publicSignature.firstNameSegment))

private val IrSymbol.publicSignature: CommonSignature
  get() =
    when (this) {
      is IrFieldSymbol -> {
        // We only expect references of backing field of properties, so the signature is a
        // CompositeSignature where the container signature is the signature of the property and the
        // inner signature is the local signature of the backing field.
        check(signature is CompositeSignature)
        // return the public signature of the property
        (signature as CompositeSignature).container.asPublic()!!
      }
      else -> signature?.asPublic()!!
    }
