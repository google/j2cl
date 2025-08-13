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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin

import com.google.common.collect.ImmutableList
import com.google.j2cl.common.SourcePosition
import com.google.j2cl.transpiler.ast.Annotation
import com.google.j2cl.transpiler.ast.ArrayConstant
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.NullabilityAnnotation
import com.google.j2cl.transpiler.ast.PackageDeclaration
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage.JAVA
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage.KOTLIN
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeDescriptors.SingletonBuilder
import com.google.j2cl.transpiler.ast.TypeDescriptors.isKotlinNothing
import com.google.j2cl.transpiler.ast.TypeLiteral
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.frontend.common.SupportedAnnotations
import com.google.j2cl.transpiler.frontend.kotlin.ir.enumEntries
import com.google.j2cl.transpiler.frontend.kotlin.ir.fqnOrFail
import com.google.j2cl.transpiler.frontend.kotlin.ir.fromQualifiedBinaryName
import com.google.j2cl.transpiler.frontend.kotlin.ir.getAllAnnotations
import com.google.j2cl.transpiler.frontend.kotlin.ir.getAllTypeParameters
import com.google.j2cl.transpiler.frontend.kotlin.ir.getJsEnumInfo
import com.google.j2cl.transpiler.frontend.kotlin.ir.getJsInfo
import com.google.j2cl.transpiler.frontend.kotlin.ir.getJsMemberAnnotationInfo
import com.google.j2cl.transpiler.frontend.kotlin.ir.getParameters
import com.google.j2cl.transpiler.frontend.kotlin.ir.getTypeSubstitutionMap
import com.google.j2cl.transpiler.frontend.kotlin.ir.hasVoidReturn
import com.google.j2cl.transpiler.frontend.kotlin.ir.isAbstract
import com.google.j2cl.transpiler.frontend.kotlin.ir.isArrayType
import com.google.j2cl.transpiler.frontend.kotlin.ir.isCapturingEnclosingInstance
import com.google.j2cl.transpiler.frontend.kotlin.ir.isClassType
import com.google.j2cl.transpiler.frontend.kotlin.ir.isFinal
import com.google.j2cl.transpiler.frontend.kotlin.ir.isFunctionalInterface
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsFunction
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsOptional
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsType
import com.google.j2cl.transpiler.frontend.kotlin.ir.isNativeJsField
import com.google.j2cl.transpiler.frontend.kotlin.ir.j2clKind
import com.google.j2cl.transpiler.frontend.kotlin.ir.j2clName
import com.google.j2cl.transpiler.frontend.kotlin.ir.j2clVisibility
import com.google.j2cl.transpiler.frontend.kotlin.ir.methods
import com.google.j2cl.transpiler.frontend.kotlin.ir.overriddenSpecialBridgeSignatures
import com.google.j2cl.transpiler.frontend.kotlin.ir.simpleSourceName
import com.google.j2cl.transpiler.frontend.kotlin.ir.singleAbstractMethod
import com.google.j2cl.transpiler.frontend.kotlin.ir.typeSubstitutionMap
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.SpecialBridge
import org.jetbrains.kotlin.backend.jvm.ir.collectVisibleTypeParameters
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.backend.jvm.ir.eraseToScope
import org.jetbrains.kotlin.backend.jvm.lower.getFileClassInfo
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getArrayElementType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.eraseTypeParameters
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideMaybeAbstractOrFail
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.types.Variance

/** Utility functions to interact with the Kotlin compiler internal representations. */
internal class KotlinEnvironment(
  private val pluginContext: IrPluginContext,
  private val packageInfoCache: PackageInfoCache,
  private val jvmBackendContext: JvmBackendContext,
) {
  private val builtinsResolver = BuiltinsResolver(pluginContext, jvmBackendContext)
  private val typeDescriptorByIrType: MutableMap<IrType, TypeDescriptor> = HashMap()
  private val typeDeclarationByIrClass: MutableMap<IrClass, TypeDeclaration> = HashMap()
  private val methodDescriptorByIrFunction: MutableMap<IrFunction, MethodDescriptor> = HashMap()
  private val fieldDescriptorByIrField: MutableMap<IrField, FieldDescriptor> = HashMap()

  init {
    initWellKnownTypes()

    mapOf(
        pluginContext.irBuiltIns.booleanType to PrimitiveTypes.BOOLEAN,
        pluginContext.irBuiltIns.byteType to PrimitiveTypes.BYTE,
        pluginContext.irBuiltIns.charType to PrimitiveTypes.CHAR,
        pluginContext.irBuiltIns.doubleType to PrimitiveTypes.DOUBLE,
        pluginContext.irBuiltIns.floatType to PrimitiveTypes.FLOAT,
        pluginContext.irBuiltIns.intType to PrimitiveTypes.INT,
        pluginContext.irBuiltIns.longType to PrimitiveTypes.LONG,
        pluginContext.irBuiltIns.shortType to PrimitiveTypes.SHORT,
      )
      .forEach {
        // Add default mappings for basic types to their corresponding Java types.
        typeDescriptorByIrType[it.key] = it.value
        typeDescriptorByIrType[it.key.makeNullable()] = it.value.toBoxedType().toNullable()

        // Add mappings for primitive arrays types.
        val arrayType = pluginContext.irBuiltIns.primitiveArrayForType[it.key]!!.defaultType
        typeDescriptorByIrType[arrayType] =
          ArrayTypeDescriptor.newBuilder()
            .setComponentTypeDescriptor(it.value)
            .setNullable(false)
            .build()
        typeDescriptorByIrType[arrayType.makeNullable()] =
          ArrayTypeDescriptor.newBuilder()
            .setComponentTypeDescriptor(it.value)
            .setNullable(true)
            .build()
      }
  }

  private fun initWellKnownTypes() {
    check(!TypeDescriptors.isInitialized())

    val builder = SingletonBuilder()

    for (name in TypeDescriptors.getWellKnownTypeNames()) {
      val typeDescriptor = getWellKnownTypeDescriptor(name)
      if (typeDescriptor != null) {
        builder.addReferenceType(typeDescriptor)
      }
    }

    builder.buildSingleton()
  }

  private fun getWellKnownTypeDescriptor(qualifiedBinaryName: String): DeclaredTypeDescriptor? {
    // Note: We disable declaration-site variance when resolving type descriptors of the well
    // known types because the variance is not needed we creating a reference to a type and using
    // variance would require resolving java.lang.Object via TypeDescriptors.get() during
    // TypeDescriptors initialization, causing a cycle.
    return pluginContext.referenceClass(ClassId.fromQualifiedBinaryName(qualifiedBinaryName))?.let {
      getDeclaredTypeDescriptor(it.defaultType.makeNullable(), useDeclarationVariance = false)
    }
  }

  fun getDeclarationForType(irClass: IrClass?): TypeDeclaration? {
    irClass ?: return null

    return typeDeclarationByIrClass.getOrPut(irClass) {
      TypeDeclaration.newBuilder()
        .setClassComponents(irClass.getClassComponents())
        .setKind(irClass.j2clKind)
        .setAnnotation(irClass.isAnnotationClass)
        .setSourceLanguage(if (irClass.isFromJava()) JAVA else KOTLIN)
        .setOriginalSimpleSourceName(irClass.simpleSourceName)
        .setPackage(createPackageDeclaration(irClass.packageFqName!!.asString()))
        .setVisibility(irClass.j2clVisibility)
        .setEnclosingTypeDeclaration(getDeclarationForType(irClass.parentClassOrNull))
        .setDeclaredMethodDescriptorsFactory { _ ->
          ImmutableList.copyOf(irClass.methods.map(::getDeclaredMethodDescriptor))
        }
        .setSingleAbstractMethodDescriptorFactory { _ ->
          irClass.singleAbstractMethod?.let(::getDeclaredMethodDescriptor)
        }
        .setDeclaredFieldDescriptorsFactory { _ ->
          ImmutableList.copyOf(
            irClass.enumEntries.map(::getDeclaredFieldDescriptor) +
              irClass.getDeclaredFields().map(::getDeclaredFieldDescriptor).toList()
          )
        }
        .setSuperTypeDescriptorFactory { _ ->
          irClass.superClass?.let { getSuperTypeDescriptor(it.makeNullable()) }
        }
        .setInterfaceTypeDescriptorsFactory { _ ->
          ImmutableList.copyOf(
            irClass.superTypes
              .filter(IrType::isInterface)
              .map(IrType::makeNullable)
              .map(::getSuperTypeDescriptor)
              // We can have duplicate types after JVM intrinsics have been resolved. See
              // b/308776304
              .distinctBy { it.typeDeclaration }
          )
        }
        .setMemberTypeDeclarationsFactory {
          ImmutableList.copyOf(
            irClass.declarations.filterIsInstance<IrClass>().map { c -> getDeclarationForType(c)!! }
          )
        }
        .setTypeParameterDescriptors(irClass.getAllTypeParameters().map(::getTypeVariable).toList())
        .setCapturingEnclosingInstance(irClass.isCapturingEnclosingInstance)
        .setFunctionalInterface(irClass.isFunctionalInterface)
        .setHasAbstractModifier(irClass.isAbstract)
        .setFinal(irClass.isFinal)
        .setLocal(irClass.isLocal && !irClass.isAnonymousObject)
        .setAnonymous(irClass.isAnonymousObject)
        .setJsType(irClass.isJsType)
        .setJsFunctionInterface(irClass.isJsFunction)
        .setJsEnumInfo(irClass.getJsEnumInfo())
        .apply {
          val jsMemberAnnotation = irClass.getJsMemberAnnotationInfo()
          setCustomizedJsNamespace(jsMemberAnnotation?.namespace)
          setSimpleJsName(jsMemberAnnotation?.name)
          setNative(jsMemberAnnotation?.isNative ?: false)
        }
        .setAnnotationsFactory { createAnnotations(irClass) }
        .build()
    }
  }

  private fun createAnnotations(irAnnotationContainer: IrAnnotationContainer) =
    ImmutableList.Builder<Annotation>()
      .apply {
        for (annotationCtorCall in irAnnotationContainer.getAllAnnotations()) {
          val ctor = annotationCtorCall.symbol.owner
          val typeDescriptor =
            checkNotNull(getEnclosingTypeDescriptor(ctor)) {
              "No enclosing type for ${ctor.dump()}"
            }
          if (!SupportedAnnotations.isSupportedAnnotation(typeDescriptor.qualifiedSourceName)) {
            continue
          }
          add(
            Annotation.newBuilder()
              .setTypeDescriptor(typeDescriptor)
              .addAnnotationValues(annotationCtorCall)
              .build()
          )
        }
      }
      .build()

  private fun Annotation.Builder.addAnnotationValues(
    annotationCtorCall: IrConstructorCall
  ): Annotation.Builder {
    fun IrExpression?.toAnnotationValue(): Literal? {
      fun createArrayConstant(type: IrType, values: List<IrExpression>): ArrayConstant? {
        val translatedValues = values.map { it.toAnnotationValue() }
        // TODO(b/397460318, b/395716783): Remove this null check once we handle all member value
        // types. We don't expect null unless it's an unhandled value type.
        if (translatedValues.contains(null)) {
          return null
        }
        return ArrayConstant.newBuilder()
          .setTypeDescriptor(createArrayTypeDescriptor(type))
          .setValueExpressions(translatedValues)
          .build()
      }

      return when (this) {
        is IrConst -> Literal.fromValue(value, getTypeDescriptor(type))
        is IrClassReference ->
          createTypeLiteral(classType, SourcePosition.NONE, wrapPrimitives = false)
        is IrConstantArray -> createArrayConstant(type, elements)
        is IrVararg ->
          createArrayConstant(
            type,
            elements.map {
              // Spread operator should be lowered by this point since only compile-time constants
              // are allowed, so we will only see IrExpression here.
              it as IrExpression
            },
          )
        // TODO(b/397460318, b/395716783): Implement various member value types, then throw an
        // exception here if unhandled.
        else -> null
      }
    }

    for (i in 0 until annotationCtorCall.valueArgumentsCount) {
      val name = annotationCtorCall.symbol.owner.valueParameters[i].name.asString()
      val translatedValue = annotationCtorCall.getValueArgument(i).toAnnotationValue() ?: continue
      addValue(name, translatedValue)
    }
    return this
  }

  private fun createPackageDeclaration(packageName: String) =
    // Caching is left to PackageDeclaration.Builder since construction is trivial.
    PackageDeclaration.newBuilder()
      .setName(packageName)
      .setCustomizedJsNamespace(packageInfoCache.getJsNamespace(packageName))
      .build()

  /**
   * Returns a type descriptor to be used as a super type in a type declaration.
   *
   * <p> When types with type declaration variance are used as super types, the variance is not
   * propagated since it is not allowed.
   */
  private fun getSuperTypeDescriptor(irType: IrType): DeclaredTypeDescriptor =
    getDeclaredType(irType as IrSimpleType, useDeclarationVariance = false)

  fun getDeclaredTypeDescriptor(
    irType: IrType,
    useDeclarationVariance: Boolean = true,
  ): DeclaredTypeDescriptor =
    getTypeDescriptor(irType, useDeclarationVariance) as DeclaredTypeDescriptor

  fun getTypeDescriptor(irType: IrType, useDeclarationVariance: Boolean = true): TypeDescriptor {
    var typeDescriptor =
      typeDescriptorByIrType.getOrPut(irType) {
        when {
          irType.isTypeParameter() -> {
            val typeParameter = irType.classifierOrNull!!.owner as IrTypeParameter
            getTypeVariable(typeParameter, !typeParameter.isFromJava() && irType.isMarkedNullable())
          }
          irType.isArrayType() -> createArrayTypeDescriptor(irType)
          irType is IrSimpleType -> getDeclaredType(irType, useDeclarationVariance)
          else -> TODO("Not supported type $irType")
        }
      }

    // TODO(b/266964795): Properly handle type declarations with unsafe variance. Only when
    // @UnsafeVariance annotates types that are directly the parameter should be replaced by the
    // erasure; for @UnsafeVariance usages in other parameterized types a wildcard should be used.
    //   contains(e: @UnsafeVariance E) -->  contains(e : Object)
    //   containsAll(c: Collection<@UnsafeVariance E> c) --> containsAll(e : Collection<out E> c)
    // TODO(b/365133427): Annotations are not accounted for hashcode and equals so the logic is
    // moved outside of cache. We should move it back once the bug is fixed.
    val annotations = irType.annotations
    if (annotations.isNotEmpty() && annotations.hasAnnotation(FqName("kotlin.UnsafeVariance"))) {
      typeDescriptor = typeDescriptor.toRawTypeDescriptor()
    }

    // If we mapped to a primitive type but the kotlin type was annotated with @EnhancedNullability
    // that means we're interoping with a non-nullable Java boxed type. Kotlin cannot represent this
    // in their type system so we need to be mindful to box the type again.
    if (typeDescriptor.isPrimitive && irType.hasAnnotation(ENHANCED_NULLABILITY_ANNOTATION)) {
      typeDescriptor = typeDescriptor.toBoxedType()
    }

    return typeDescriptor
  }

  fun getReferenceTypeDescriptor(irType: IrType): TypeDescriptor {
    val typeDescriptor = getTypeDescriptor(irType)
    // In reference context, primitive types are mapped to their boxed variants.
    return if (typeDescriptor.isPrimitive) typeDescriptor.toBoxedType() else typeDescriptor
  }

  // TODO(b/287681086): Review which cases need the propagation of the declaration variance, in the
  //  case of method references the more precise type is needed since the type is used to decided
  //  how to match varargs methods.
  fun getReferenceTypeDescriptorForFunctionReference(irType: IrSimpleType) =
    getDeclaredType(irType, useDeclarationVariance = false)

  fun getReferenceTypeDescriptorForTypeArgument(irTypeArgument: IrTypeArgument): TypeDescriptor =
    when (irTypeArgument) {
      is IrStarProjection -> TypeVariable.createWildcard()
      is IrTypeProjection -> {
        val type = getReferenceTypeDescriptor(irTypeArgument.typeOrNull!!)
        when (irTypeArgument.variance) {
          Variance.INVARIANT -> type
          Variance.IN_VARIANCE -> TypeVariable.createWildcardWithLowerBound(type)
          Variance.OUT_VARIANCE -> TypeVariable.createWildcardWithUpperBound(type)
        }
      }
      else -> TODO("Not supported type argument $irTypeArgument")
    }

  private fun getTypeVariable(
    irTypeParameter: IrTypeParameter,
    isNullable: Boolean = false,
  ): TypeVariable {
    val upperBoundFactory =
      if (irTypeParameter.superTypes.size == 1) {
        { getReferenceTypeDescriptor(irTypeParameter.superTypes.single()) }
      } else {
        { createIntersectionTypeDescriptor(irTypeParameter.superTypes) }
      }

    return TypeVariable.newBuilder()
      .setName(irTypeParameter.name.asString())
      .setUniqueKey(irTypeParameter.uniqueKey)
      .setUpperBoundTypeDescriptorFactory(upperBoundFactory)
      .setNullabilityAnnotation(
        if (isNullable) NullabilityAnnotation.NULLABLE else NullabilityAnnotation.NONE
      )
      .build()
  }

  private fun createIntersectionTypeDescriptor(types: List<IrType>): IntersectionTypeDescriptor =
    IntersectionTypeDescriptor.newBuilder()
      .setIntersectionTypeDescriptors(types.map(::getReferenceTypeDescriptor))
      .build()

  private fun createArrayTypeDescriptor(arrayType: IrType) =
    ArrayTypeDescriptor.newBuilder()
      .setComponentTypeDescriptor(
        getReferenceTypeDescriptor(arrayType.getArrayElementType(pluginContext.irBuiltIns))
      )
      .setNullable(arrayType.isNullable())
      .build()

  private fun getDeclaredType(
    originalType: IrSimpleType,
    useDeclarationVariance: Boolean,
  ): DeclaredTypeDescriptor {
    val irType = originalType.resolveBuiltinClass(useDeclarationVariance)
    return getDeclarationForType(irType.getClass())!!
      .toDescriptor()
      .specializeTypeDescriptor(irType, useDeclarationVariance)
  }

  private fun DeclaredTypeDescriptor.specializeTypeDescriptor(
    irType: IrSimpleType,
    useDeclarationVariance: Boolean,
  ): DeclaredTypeDescriptor {
    // Adjust nullability.
    var td = if (irType.isNullable()) toNullable() else toNonNullable()
    if (irType.arguments.isNotEmpty()) {
      // Adjust type arguments.
      val subsitutionMap =
        irType.getTypeSubstitutionMap(useDeclarationVariance).toTypeDescriptorByTypeVariableMap()
      td = td.specializeTypeVariables(subsitutionMap) as DeclaredTypeDescriptor
    }
    return td
  }

  fun getMethodDescriptor(
    functionDeclaration: IrFunction,
    typeArgumentsByTypeParameter: Map<IrTypeParameterSymbol, IrTypeArgument>,
  ): MethodDescriptor {
    var resolvedFunctionDeclaration = functionDeclaration
    var cumulativeTypeArgumentsByTypeParameter = typeArgumentsByTypeParameter

    if (functionDeclaration.isFakeOverride) {
      // Resolve the target when it is a synthetic bridge inserted by the frontend.
      resolvedFunctionDeclaration =
        (functionDeclaration as IrSimpleFunction).resolveFakeOverrideMaybeAbstractOrFail()

      // Remap type parameters from the fake override function to the resolved function.
      cumulativeTypeArgumentsByTypeParameter =
        resolveTypeParametersForFunction(
          cumulativeTypeArgumentsByTypeParameter,
          functionDeclaration,
          resolvedFunctionDeclaration,
        )

      // Since the resolved target has lost all parameterization, compute all the type variable
      // assignments up the hierarchy.
      cumulativeTypeArgumentsByTypeParameter =
        propagateSubstitutions(
          functionDeclaration.parentClassOrNull!!.superTypes,
          cumulativeTypeArgumentsByTypeParameter,
        )
    }
    require(!resolvedFunctionDeclaration.isFakeOverride)

    val declarationMethodDescriptor = getDeclaredMethodDescriptor(resolvedFunctionDeclaration)

    if (cumulativeTypeArgumentsByTypeParameter.isEmpty()) return declarationMethodDescriptor
    return declarationMethodDescriptor.specializeTypeVariables(
      cumulativeTypeArgumentsByTypeParameter.toTypeDescriptorByTypeVariableMap()
    )
  }

  private fun resolveTypeParametersForFunction(
    originalTypeMapping: Map<IrTypeParameterSymbol, IrTypeArgument>,
    originalFunction: IrFunction,
    resolvedFunction: IrFunction,
  ): Map<IrTypeParameterSymbol, IrTypeArgument> {
    val resolvedMethodTypeParamsByIndex = resolvedFunction.typeParameters.associateBy { it.index }
    val originalToResolvedTypeParameters =
      originalFunction.typeParameters.associateWith { resolvedMethodTypeParamsByIndex[it.index]!! }

    return originalTypeMapping.toMutableMap().apply {
      for ((originalTypeParam, resolvedTypeParam) in originalToResolvedTypeParameters) {
        put(resolvedTypeParam.symbol, this[originalTypeParam.symbol]!!)
      }
    }
  }

  /**
   * Propagates the type substitution upwards in the hierarchy and collects all type variable
   * mappings.
   *
   * Note that this algorithm is inefficient as it traverses and collect among all supertypes. It
   * would suffice to propagate just on the path up the enclosing type of the resolved override.
   * However, this is only used for resolving fake overrides which are a very small minority of the
   * method descriptors.
   */
  private fun propagateSubstitutions(
    irTypes: List<IrType>,
    typeArgumentsByTypeParameter: Map<IrTypeParameterSymbol, IrTypeArgument>,
  ): Map<IrTypeParameterSymbol, IrTypeArgument> {
    var cumulativeTypeArgumentsByTypeParameter = typeArgumentsByTypeParameter
    for (superType in irTypes) {
      // Apply the current parameterization so that it gets propagated when taking the super types.
      val parameterizedSuperType =
        IrTypeSubstitutor(cumulativeTypeArgumentsByTypeParameter).substitute(superType)
      cumulativeTypeArgumentsByTypeParameter += parameterizedSuperType.typeSubstitutionMap
      cumulativeTypeArgumentsByTypeParameter =
        propagateSubstitutions(
          parameterizedSuperType.superTypes(),
          cumulativeTypeArgumentsByTypeParameter,
        )
    }
    return cumulativeTypeArgumentsByTypeParameter
  }

  /**
   * For functions coming from Java, computes the specialized bridge needed to satisfy JRE types.
   *
   * This is useful for reverse mapping APIs from Kotlin standard library types to the types that
   * actually exist in the JRE.
   */
  private fun computeSpecialBridgeIfNeeded(irFunction: IrFunction): SpecialBridge? {
    if (irFunction !is IrSimpleFunction || !irFunction.isFromJava()) return null
    val specialBridge =
      jvmBackendContext.bridgeLoweringCache.computeSpecialBridge(irFunction) ?: return null

    // If our signature matches one of the overridden signatures, we don't need a bridge.
    if (
      specialBridge.signature in overriddenSpecialBridgeSignatures(jvmBackendContext, irFunction)
    ) {
      return null
    }

    return specialBridge
  }

  fun getDeclaredMethodDescriptor(irFunction: IrFunction): MethodDescriptor {
    return methodDescriptorByIrFunction.getOrPut(irFunction) {
      val resolvedSymbol = builtinsResolver.resolveFunctionSymbol(irFunction.symbol)
      if (resolvedSymbol != irFunction.symbol)
        return@getOrPut getDeclaredMethodDescriptor(resolvedSymbol.owner)

      // Check if a bridge would be need for this special function if it's coming from Java. If so
      // we'll use it to reverse compute the types that actually exist in the Java method.
      val specialBridge = computeSpecialBridgeIfNeeded(irFunction)
      val visibleTypeParameters = collectVisibleTypeParameters(irFunction)

      val enclosingTypeDescriptor =
        checkNotNull(getEnclosingTypeDescriptor(irFunction)) {
          "No enclosing type for ${irFunction.dump()}"
        }

      val isConstructor = irFunction is IrConstructor
      val parameterDescriptors = ImmutableList.builder<MethodDescriptor.ParameterDescriptor>()

      if (irFunction.isSuspend) {
        // Add the implicit continuation parameter so our type model is correct.
        parameterDescriptors.add(
          MethodDescriptor.ParameterDescriptor.newBuilder()
            .setTypeDescriptor(
              TypeDescriptors.get()
                .kotlinCoroutinesContinuation!!
                .withTypeArguments(ImmutableList.of(TypeDescriptors.getUnknownType()))
            )
            .build()
        )
      }

      val parameters =
        if (specialBridge == null) irFunction.getParameters()
        else specialBridge.overridden.getParameters()

      parameters.withIndex().forEach { (index, param) ->
        var type = param.type
        if (specialBridge != null) {
          // If there's a known type to use that, but erase be mindful of captured type parameters.
          // Otherwise, erase all type parameters to match the original JRE types.
          val substitutedType =
            specialBridge.substitutedParameterTypes?.get(param.indexInParameters)
          type = substitutedType?.eraseToScope(visibleTypeParameters) ?: type.eraseTypeParameters()
        }
        parameterDescriptors.add(
          MethodDescriptor.ParameterDescriptor.newBuilder()
            .setTypeDescriptor(getTypeDescriptor(type))
            .setJsOptional(param.isJsOptional)
            .setAnnotations(createAnnotations(param))
            .setVarargs(index == parameters.lastIndex && param.isVararg)
            .build()
        )
      }

      val visibility = irFunction.j2clVisibility
      val isStatic = (irFunction.isStatic || irFunction.parent !is IrDeclaration) && !isConstructor
      val isNative =
        irFunction.isExternal ||
          (!irFunction.getJsInfo().isJsOverlay &&
            enclosingTypeDescriptor.isNative &&
            irFunction.isAbstract)
      val isLocal = irFunction.visibility.delegate == Visibilities.Local
      val enclosingMethodDescriptor =
        if (isLocal) getDeclaredMethodDescriptor(irFunction.parent as IrFunction) else null

      MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setEnclosingMethodDescriptor(enclosingMethodDescriptor)
        .setName(irFunction.j2clName(jvmBackendContext))
        .setParameterDescriptors(parameterDescriptors.build())
        .setReturnTypeDescriptor(
          if (irFunction.hasVoidReturn) {
            PrimitiveTypes.VOID
          } else {
            var returnType = irFunction.returnType
            if (specialBridge != null) {
              // If there's a known type to use that, but erase be mindful of captured type
              // parameters. Otherwise, erase all type parameters to match the original JRE types.
              returnType =
                specialBridge.substitutedReturnType?.eraseToScope(irFunction.parentAsClass)
                  ?: specialBridge.overridden.returnType.eraseTypeParameters()
            }
            getTypeDescriptor(returnType)
          }
        )
        .setVisibility(visibility)
        .setConstructor(isConstructor)
        .setStatic(isStatic)
        .setAbstract(irFunction.isAbstract)
        .setFinal(irFunction.isFinal)
        .setNative(isNative)
        .setDefaultMethod(
          enclosingTypeDescriptor.isInterface &&
            !irFunction.isAbstract &&
            !visibility.isPrivate &&
            !isStatic
        )
        .setTypeParameterTypeDescriptors(irFunction.typeParameters.map(::getTypeVariable))
        .setOriginalJsInfo(irFunction.getJsInfo())
        .setAnnotations(createAnnotations(irFunction))
        .setSuspendFunction(irFunction.isSuspend)
        .build()
    }
  }

  fun getFieldDescriptor(
    field: IrField,
    typeArgumentsByTypeParameter: Map<IrTypeParameterSymbol, IrTypeArgument>,
  ): FieldDescriptor {
    val declarationFieldDescriptor = getDeclaredFieldDescriptor(field)

    if (typeArgumentsByTypeParameter.isEmpty()) return declarationFieldDescriptor
    return declarationFieldDescriptor.specializeTypeVariables(
      typeArgumentsByTypeParameter.toTypeDescriptorByTypeVariableMap()
    )
  }

  fun getDeclaredFieldDescriptor(irField: IrField): FieldDescriptor {
    return fieldDescriptorByIrField.getOrPut(irField) {
      val resolvedSymbol = builtinsResolver.resolveFieldSymbol(irField.symbol)
      if (resolvedSymbol != irField.symbol)
        return@getOrPut getDeclaredFieldDescriptor(resolvedSymbol.owner)

      val fieldTypeDescriptor = getTypeDescriptor(irField.type)

      var constantValue =
        irField.constantValue()?.value?.let { Literal.fromValue(it, fieldTypeDescriptor) }
      if (
        constantValue == null &&
          !irField.isFromJava() &&
          irField.correspondingPropertySymbol?.owner?.isConst == true
      ) {
        // In Kotlin, const val initialized to their default value does not have initializer and
        // `irField.constantValue()` returns null. In that case use the default value of the field
        // type.
        constantValue = fieldTypeDescriptor.defaultValue
      }

      FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(getEnclosingTypeDescriptor(irField))
        .setName(irField.name.asString())
        .setTypeDescriptor(fieldTypeDescriptor)
        .setVisibility(irField.j2clVisibility)
        .setCompileTimeConstant(constantValue != null)
        .setConstantValue(constantValue)
        // Kotlin has stricter requirements for val properties than exists for Java final fields;
        // thus  we allow val properties to be native JS members, but we pretend like it's non-final
        // in the  J2CL AST to not fail JsInterop restriction checks later on. Weaken the final
        // semantics here should cause a practical problem as the JVM compilations would have
        // already enforced the final semantics.
        .setFinal(irField.isFinal && !irField.isNativeJsField)
        .setStatic(irField.isStatic || irField.parent !is IrDeclaration)
        .setOriginalJsInfo(irField.getJsInfo())
        .setAnnotations(createAnnotations(irField))
        .build()
    }
  }

  fun getDeclaredFieldDescriptor(irEnumEntry: IrEnumEntry): FieldDescriptor =
    FieldDescriptor.newBuilder()
      .setEnclosingTypeDescriptor(getEnclosingTypeDescriptor(irEnumEntry))
      .setName(irEnumEntry.name.asString())
      .setTypeDescriptor(getEnclosingTypeDescriptor(irEnumEntry)!!.toNonNullable())
      .setVisibility(Visibility.PUBLIC)
      .setFinal(true)
      .setStatic(true)
      .setEnumConstant(true)
      .setOriginalJsInfo(irEnumEntry.getJsInfo())
      .setAnnotations(createAnnotations(irEnumEntry))
      .build()

  private fun getEnclosingTypeDescriptor(irDeclaration: IrDeclaration): DeclaredTypeDescriptor? =
    irDeclaration.parentClassOrNull?.let {
      getDeclaredTypeDescriptor(it.defaultType.makeNullable())
    }

  fun createTypeLiteral(
    irType: IrType,
    sourcePosition: SourcePosition,
    wrapPrimitives: Boolean,
  ): TypeLiteral =
    TypeLiteral(
      sourcePosition,
      getTypeDescriptor(irType).let {
        when {
          // "Nothing" is a special case as we thread it through the J2CL AST as a stubbed type. In
          // the context of Nothing::class it should be treated Void.
          TypeDescriptors.isKotlinNothing(it) -> TypeDescriptors.get().javaLangVoid
          it.isPrimitive && wrapPrimitives -> it.toBoxedType()
          else -> it
        }
      },
    )

  private val IrType.superClass: IrType?
    get() {
      // In Kotlin, interfaces extends Any, but conceptually they do not have super class.
      // Any is the root of the Kotlin class hierarchy and do not have super class.
      // As we currently map Any to j.l.Object, we need to special case j.l.Object to not have Any
      // as super type and avoid a loop in type hierarchy.
      // Annotations are not interfaces in Kotlin but they are in our model.
      if (
        isInterface() || isAnnotation() || isAny() || isClassType(FqNameUnsafe("java.lang.Object"))
      ) {
        return null
      }
      // As interfaces extend Any, a class implementing interfaces does not have Any in its direct
      // superTypes list.
      return superTypes().singleOrNull { !it.isInterface() } ?: pluginContext.irBuiltIns.anyType
    }

  private val IrClass.superClass: IrType?
    get() = defaultType.superClass

  /** Keeps track of the unique names for local classes */
  private val classComponentsByLocalIrClass: MutableMap<IrClass, List<String>> = HashMap()
  private val syntheticClassNumberByEnclosingClass: MutableMap<String, Int> = HashMap()

  private fun IrClass.getClassComponents(): List<String> {
    return if (isLocal) {
      // For local classes, keep a cache track of the assigned class components to them since
      // they are synthetic.
      // Names for local classes are prepended a number making sure the full qualified name
      // for the class is unique.
      classComponentsByLocalIrClass.getOrPut(this) {
        val enclosingParentClassComponent =
          parentClassOrNull?.getClassComponents() ?: file.getClassComponents()
        val name = if (isAnonymousObject) "" else name.asString()
        val namingKey = (parentClassOrNull ?: file).kotlinFqName.asString() + "." + name
        val index = (syntheticClassNumberByEnclosingClass[namingKey] ?: 0) + 1
        syntheticClassNumberByEnclosingClass[namingKey] = index
        enclosingParentClassComponent + "${index}$name"
      }
    } else {
      classId!!.relativeClassName.pathSegments().map { it.asString() }
    }
  }

  private fun IrPackageFragment.getClassComponents(): List<String> =
    when (this) {
      is IrFile -> listOf(getFileClassInfo().fileClassFqName.shortName().asString())
      // builtin top level functions are not part of a file but the package kotlin
      else -> listOf(packageFqName.shortName().asString())
    }

  private fun IrTypeParameter.getClassComponents(): List<String> {
    return buildList {
      val enclosingClass = parentClassOrNull
      if (enclosingClass == null) {
        error("IrTypeParameter has no enclosing class: ${this@getClassComponents.dump()}")
      }
      // Ensure we resolve the class before getting its package path. Otherwise we can end up with
      // mismatched package paths depending upon the context.
      val resolvedEnclosingClass =
        builtinsResolver.resolveClass(enclosingClass.symbol)?.owner ?: enclosingClass
      add(resolvedEnclosingClass.packageFqName!!.pathSegments().joinToString("/"))
      addAll(resolvedEnclosingClass.getClassComponents())
      add(
        when (parent) {
          is IrClass -> "C_${name.asString()}"
          is IrDeclarationWithName ->
            "M_${(parent as IrDeclarationWithName).name.asString()}_${name.asString()}"
          else -> throw IllegalStateException("Unknown parent kind $parent")
        }
      )
    }
  }

  private val IrTypeParameter.uniqueKey: String
    get() =
      getClassComponents().joinToString("::") +
        superTypes.joinToString("|") { it.fqnOrFail.asString() }

  private fun Map<IrTypeParameterSymbol, IrTypeArgument>.toTypeDescriptorByTypeVariableMap() =
    entries.associate {
      getTypeVariable(it.key.owner) to getReferenceTypeDescriptorForTypeArgument(it.value)
    }

  private fun IrSimpleType.resolveBuiltinClass(
    useDeclarationVariance: Boolean = true
  ): IrSimpleType {
    val classSymbol = classOrNull ?: return this
    val builtinClass = builtinsResolver.resolveClass(classSymbol) ?: return this

    val originalTypeParams = classSymbol.owner.typeParameters
    val replacementTypeParams = builtinClass.owner.typeParameters

    check(
      (originalTypeParams.size == replacementTypeParams.size &&
        arguments.size == replacementTypeParams.size) || this.isKFunction()
    ) {
      """
      |Mismatch in number of parameters for original type ${classSymbol.owner.kotlinFqName} mapped
      |to type ${builtinClass.owner.kotlinFqName}.
      |Original number of parameters: ${originalTypeParams.size}
      |Replacement number of parameters: ${replacementTypeParams.size}
      |Number of arguments: ${arguments.size}
      """
        .trimMargin()
    }

    // Rewrite the type arguments to account for mismatches in declaration variance, only if we
    // would've respected the declaration variance.
    val replacementArguments =
      when {
        !useDeclarationVariance -> arguments
        // KFunction is a special case where there's a mismatch in the number of original type
        // params,
        // replacement type params, and number of arguments. We're not going to attempt to rewrite
        // these.
        isKFunction() -> arguments
        else ->
          remapDeclarationTypeVarianceOntoArguments(
            arguments,
            originalTypeParams,
            replacementTypeParams,
          )
      }

    // Return a new copy of the type with the builtin class symbol in place of the original symbol.
    return IrSimpleTypeImpl(
      builtinClass,
      nullability,
      replacementArguments,
      annotations,
      abbreviation,
    )
  }

  fun IrClass.getDeclaredFields(): Set<IrField> {
    var fields = declarations.filterIsInstance<IrField>().filter { it.isReal }.toSet()

    val companion = declarations.filterIsInstance<IrClass>().firstOrNull(IrClass::isCompanion)
    if (companion != null) {
      // When the IrClass is built from bytecode, kotlinc emits the field containing the unique
      // instance the companion of the class because it's an implementation detail of the Kotlin/JVM
      // backend. We will fix the glitch here in order to have a consistent model on the J2CL side.
      // TODO(b/335000000): Remove this if we store Kotlin metadata in our type model.
      fields += jvmBackendContext.cachedDeclarations.getFieldForObjectInstance(companion)
    }

    if (isFromJava()) {
      // Fields from Java class are represented as Kotlin properties.
      fields += declarations.filterIsInstance<IrProperty>().mapNotNull(IrProperty::backingField)
    }
    return fields
  }
}

private fun remapDeclarationTypeVarianceOntoArguments(
  arguments: List<IrTypeArgument>,
  originalTypeParams: List<IrTypeParameter>,
  replacementTypeParams: List<IrTypeParameter>,
) =
  arguments.mapIndexed { index, argument ->
    if (
      argument is IrTypeProjection &&
        originalTypeParams[index].variance != Variance.INVARIANT &&
        replacementTypeParams[index].variance == Variance.INVARIANT &&
        argument.variance == Variance.INVARIANT
    ) {
      makeTypeProjection(argument.type, originalTypeParams[index].variance)
    } else {
      argument
    }
  }
