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
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.Kind
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeDescriptors.SingletonBuilder
import com.google.j2cl.transpiler.frontend.common.FrontendConstants
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getSuperClass
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.ClassId

/** Utility functions to interact with the Kotlin compiler internal representations. */
class KotlinEnvironment(private val components: Fir2IrComponents) {
  private val declaredTypeDescriptorByTypeBinding: MutableMap<IrType, DeclaredTypeDescriptor> =
    HashMap()
  private val primitivesTypeDescriptorByTypeBinding: Map<IrType, PrimitiveTypeDescriptor>

  init {
    initWellKnownTypes(components)

    val builtIns = components.irBuiltIns

    // Map known kotlin stdlib types to corresponding java types/primitives.
    declaredTypeDescriptorByTypeBinding +=
      mapOf(
        builtIns.anyType to TypeDescriptors.get().javaLangObject.toNonNullable(),
        builtIns.anyNType to TypeDescriptors.get().javaLangObject,
        builtIns.stringType to TypeDescriptors.get().javaLangString
      )
    primitivesTypeDescriptorByTypeBinding =
      mapOf(
        builtIns.booleanType to PrimitiveTypes.BOOLEAN,
        builtIns.byteType to PrimitiveTypes.BYTE,
        builtIns.charType to PrimitiveTypes.CHAR,
        builtIns.doubleType to PrimitiveTypes.DOUBLE,
        builtIns.floatType to PrimitiveTypes.FLOAT,
        builtIns.intType to PrimitiveTypes.INT,
        builtIns.longType to PrimitiveTypes.LONG,
        builtIns.shortType to PrimitiveTypes.SHORT,
        builtIns.unitType to PrimitiveTypes.VOID
      )
  }

  private fun initWellKnownTypes(components: Fir2IrComponents) {
    if (TypeDescriptors.isInitialized()) {
      return
    }

    val builder = SingletonBuilder()
    PrimitiveTypes.TYPES.forEach {
      builder.addPrimitiveBoxedTypeDescriptorPair(
        it,
        getDeclaredTypeDescriptor(lookupType(it.boxedClassName, components))
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
      builder.addReferenceType(getDeclaredTypeDescriptor(lookupType(it, components)))
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

    val declaredMethods = {
      ImmutableList.copyOf(
        irClass.declarations.filterIsInstance<IrFunction>().mapNotNull(::getMethodDescriptor)
      )
    }
    val declaredFields = {
      ImmutableList.copyOf(
        irClass.declarations.filterIsInstance<IrProperty>().mapNotNull(::getFieldDescriptor)
      )
    }
    val superClass = irClass.getSuperClass(components.irBuiltIns)
    val superTypeDescriptor = superClass?.let { getDeclaredTypeDescriptor(it.defaultType) }

    return TypeDeclaration.newBuilder()
      .setClassComponents(getClassComponents(irClass))
      .setKind(Kind.CLASS)
      .setPackageName(irClass.packageFqName!!.asString())
      .setVisibility(irClass.getJ2clVisibility())
      .setDeclaredMethodDescriptorsFactory(declaredMethods)
      .setDeclaredFieldDescriptorsFactory(declaredFields)
      .setSuperTypeDescriptorFactory { _ -> superTypeDescriptor }
      .build()
  }

  fun getTypeDescriptor(irType: IrType): TypeDescriptor {
    if (primitivesTypeDescriptorByTypeBinding.containsKey(irType)) {
      return primitivesTypeDescriptorByTypeBinding[irType]!!
    }
    // TODO(dramaix) add support for other TypeDescriptors.
    return getDeclaredTypeDescriptor(irType)
  }

  fun getMethodDescriptor(irFunction: IrFunction?): MethodDescriptor? {
    irFunction ?: return null

    val enclosingType = irFunction.parentClassOrNull?.defaultType
    enclosingType ?: throw NotImplementedError("Top level functions not supported yet")
    val enclosingTypeDescriptor = getDeclaredTypeDescriptor(enclosingType)
    val isConstructor = irFunction is IrConstructor
    val parameterDescriptors = ImmutableList.builder<MethodDescriptor.ParameterDescriptor>()

    irFunction.valueParameters.forEach {
      // TODO(dramaix): add support for varargs
      parameterDescriptors.add(
        MethodDescriptor.ParameterDescriptor.newBuilder()
          .setTypeDescriptor(getTypeDescriptor(it.type))
          .setJsOptional(false)
          .build()
      )
    }

    return MethodDescriptor.newBuilder()
      .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
      .setName(if (isConstructor) null else irFunction.name.asString())
      .setParameterDescriptors(parameterDescriptors.build())
      .setReturnTypeDescriptor(
        if (isConstructor) enclosingTypeDescriptor else getTypeDescriptor(irFunction.returnType)
      )
      .setVisibility(irFunction.getJ2clVisibility())
      .setConstructor(isConstructor)
      .build()
  }

  fun getFieldDescriptor(irProperty: IrProperty): FieldDescriptor? {
    val irField = irProperty.backingField ?: return null

    val enclosingType = irField.parentClassOrNull?.defaultType
    enclosingType ?: throw NotImplementedError("Top level field not supported yet")

    return FieldDescriptor.newBuilder()
      .setEnclosingTypeDescriptor(getDeclaredTypeDescriptor(enclosingType))
      .setName(irField.name.asString())
      .setTypeDescriptor(getTypeDescriptor(irField.type))
      .setVisibility(irField.getJ2clVisibility())
      .setFinal(irProperty.isConst)
      .setStatic(irField.isStatic)
      .build()
  }

  private fun getDeclaredTypeDescriptor(irType: IrType): DeclaredTypeDescriptor {
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
