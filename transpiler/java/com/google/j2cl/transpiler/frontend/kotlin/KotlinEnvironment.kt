/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.frontend.kotlin

import com.google.common.collect.ImmutableList
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Kind
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeDescriptors.SingletonBuilder
import com.google.j2cl.transpiler.frontend.common.FrontendConstants
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.ClassId

/** Utility functions to interact with the Kotlin compiler internal representations. */
class KotlinEnvironment(components: Fir2IrComponents) {
  private val declaredTypeDescriptorByTypeBinding: MutableMap<IrType, DeclaredTypeDescriptor> =
    HashMap()

  init {
    initWellKnownTypes(components)
  }

  private fun initWellKnownTypes(components: Fir2IrComponents) {
    if (TypeDescriptors.isInitialized()) {
      return
    }

    val builder = SingletonBuilder()
    PrimitiveTypes.TYPES.forEach {
      builder.addPrimitiveBoxedTypeDescriptorPair(
        it,
        getDeclaredType(lookupType(it.boxedClassName, components))
      )
    }

    // The pass implementing clinit requires a full TypeDescriptor for j.l.Runnable. We will remove
    // this hand-rolled type descriptor when members functional interface will be reflected in the
    // TypeDescriptor. Alternatively, we could simply change the pass to use a JsFunction type
    // descriptor defined in TypeDescritors instead of java.lang.runnable
    // TODO(dramaix): remove when the pass implementing clinit uses a JsFunction type descriptor
    //  instead
    builder.addReferenceType(createJavaLangRunnableDeclaration())

    FrontendConstants.REQUIRED_QUALIFIED_BINARY_NAMES.forEach {
      builder.addReferenceType(getDeclaredType(lookupType(it, components)))
    }

    builder.buildSingleton()
  }

  // TODO(dramaix): remove when TypeDescriptor are correctly defined.
  private fun createJavaLangRunnableDeclaration(): DeclaredTypeDescriptor {
    val methodSupplier = { typeDescriptor: DeclaredTypeDescriptor ->
      MethodDescriptor.newBuilder()
        .setName("run")
        .setEnclosingTypeDescriptor(typeDescriptor)
        .build()
    }

    return DeclaredTypeDescriptor.newBuilder()
      .setTypeDeclaration(
        TypeDeclaration.newBuilder()
          .setPackageName("java.lang")
          .setClassComponents(ImmutableList.of("Runnable"))
          .setKind(Kind.INTERFACE)
          .setFunctionalInterface(true)
          .setDeclaredMethodDescriptorsFactory { t ->
            ImmutableList.of(methodSupplier.invoke(t.toRawTypeDescriptor()))
          }
          .build()
      )
      .setSingleAbstractMethodDescriptorFactory(methodSupplier)
      .build()
  }

  fun getDeclarationForType(irClass: IrClass?): TypeDeclaration? {
    irClass ?: return null

    return TypeDeclaration.newBuilder()
      .setClassComponents(getClassComponents(irClass))
      .setKind(Kind.CLASS)
      .setPackageName(irClass.packageFqName!!.asString())
      .setVisibility(irClass.getJ2clVisibility())
      .build()
  }

  private fun getDeclaredType(irType: IrType): DeclaredTypeDescriptor {
    return declaredTypeDescriptorByTypeBinding.getOrPut(irType) {
      DeclaredTypeDescriptor.newBuilder()
        .setTypeDeclaration(getDeclarationForType(irType.getClass()))
        .build()
    }
  }

  private fun getClassComponents(irClass: IrClass): List<String> {
    val classId = irClass.classId
    classId ?: return emptyList()

    return classId.relativeClassName.pathSegments().map { it.asString() }
  }
}

private fun lookupType(binaryName: String, components: Fir2IrComponents): IrType {
  // java.lang.Map$Entry needs to be passed as java/lang/Map.Entry to build the correct ClassId
  val classId = ClassId.fromString(binaryName.replace('.', '/').replace('$', '.'))
  // IrBuiltIns exposes the method findClass() but it only works with top level class because we
  // cannot pass a ClassId. The IrBuiltIns implementation (IrBuiltInsOverFir) has a more generic
  // private method to load top level and inner class. We will reimplement this method here as
  // the implementation is very small.
  val firSymbol = components.session.symbolProvider.getClassLikeSymbolByFqName(classId)
  checkNotNull(firSymbol) { "Unknown class $binaryName" }
  return components.classifierStorage.getIrClassSymbol(firSymbol as FirClassSymbol).defaultType
}
