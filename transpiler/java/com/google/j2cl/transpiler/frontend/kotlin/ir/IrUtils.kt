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

package com.google.j2cl.transpiler.frontend.kotlin.ir

import com.google.common.base.CaseFormat
import com.google.j2cl.common.SourcePosition
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.DO_NOT_AUTOBOX_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.UNCHECKED_CAST_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.WASM_ANNOTATION_NAME
import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.fullValueParameterList
import org.jetbrains.kotlin.backend.jvm.getRequiresMangling
import org.jetbrains.kotlin.backend.jvm.ir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.backend.jvm.ir.getSingleAbstractMethod
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isClassType
import org.jetbrains.kotlin.ir.types.isNullableArray
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isOverridableOrOverrides
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getJvmMethodNameIfSpecial
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinWithDifferentJvmName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

/** Returns the actual function expression from inside a nested type operator. */
fun IrTypeOperatorCall.unfoldExpression(): IrExpression =
  // TODO(b/225955286): Fix to handle the general case.
  argument.let {
    when (it) {
      is IrTypeOperatorCall -> it.unfoldExpression()
      is IrFunctionExpression,
      is IrFunctionReference,
      is IrPropertyReference -> it
      else -> throw IllegalStateException("Unsupported arguments type")
    }
  }

val IrClass.j2clKind: Kind
  get() =
    when (kind) {
      // Enum entry with class body
      ClassKind.ENUM_ENTRY,
      ClassKind.CLASS,
      ClassKind.OBJECT -> Kind.CLASS
      // Kotlin's annotation classes are represented as an @interface in Java.
      ClassKind.ANNOTATION_CLASS,
      ClassKind.INTERFACE -> Kind.INTERFACE
      ClassKind.ENUM_CLASS -> Kind.ENUM
    }

val IrClass.j2clIsAnnotation: Boolean
  get() = kind == ClassKind.ANNOTATION_CLASS

val IrDeclarationWithVisibility.j2clVisibility: Visibility
  get() =
    when (visibility.delegate) {
      // Internal means that the owner is visible inside the kotlin module. When you call       //
      // kotlin members from java, they are considered as public.
      Visibilities.Public,
      Visibilities.Internal -> Visibility.PUBLIC
      Visibilities.Protected,
      JavaVisibilities.ProtectedAndPackage,
      JavaVisibilities.ProtectedStaticVisibility -> Visibility.PROTECTED
      JavaVisibilities.PackageVisibility -> Visibility.PACKAGE_PRIVATE
      else -> Visibility.PRIVATE
    }

val IrDeclarationContainer.methods: List<IrFunction>
  get() = declarations.filterIsInstance<IrFunction>().filter { it.isReal } + gettersAndSetters

private val IrDeclarationContainer.gettersAndSetters: List<IrFunction>
  get() =
    properties
      // We only care about getter/setters for real properties, and only if it's not a @JvmField,
      // as they won't have these functions generated for them.
      .filter { it.isReal && !it.backingField.isJvmField }
      .flatMap { sequenceOf(it.getter, it.setter) }
      .filterNotNull()
      .toList()

val IrField?.isJvmField: Boolean
  get() = this != null && hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

val IrClass.enumEntries: List<IrEnumEntry>
  get() = declarations.filterIsInstance<IrEnumEntry>()

fun IrFunctionAccessExpression.getArguments(): List<IrExpression> = buildList {
  addIfNotNull(extensionReceiver)
  // Enums that take no explicit params don't include the name/ordinal params in the constructor
  // calls. If all the value parameters are null we can simply return.
  if (
    this@getArguments is IrEnumConstructorCall &&
      (0 until valueArgumentsCount).all { getValueArgument(it) == null }
  ) {
    return@buildList
  }
  for (i in 0 until valueArgumentsCount) {
    val valueArgument = getValueArgument(i)
    // Value arguments should never be null at this point as default arguments and varargs have
    // already been lowered.
    checkNotNull(valueArgument) { "Unexpected null value argument" }
    add(valueArgument)
  }
}

val IrFunctionAccessExpression.isSuperCall
  get() = (this as? IrCall)?.superQualifierSymbol != null

fun IrFunction.getParameters(): List<IrValueParameter> =
  when (this) {
    is IrSimpleFunction ->
      buildList {
        addIfNotNull(extensionReceiverParameter)
        addAll(valueParameters)
      }
    else -> valueParameters
  }

// Based on org.jetbrains.kotlin.ir.types.typeConstructorParameters, but we collect type parameters
// defined on parents of anonymous object.
fun IrClass.getAllTypeParameters(): Sequence<IrTypeParameter> =
  generateSequence(this as IrTypeParametersContainer) { current ->
      val parent = current.parent as? IrTypeParametersContainer
      when {
        parent is IrClass && current is IrClass && !current.isInner -> null
        else -> parent
      }
    }
    .flatMap { it.typeParameters }

// Adapted from org/jetbrains/kotlin/ir/util/IrUtils.kt to map IrTypeParameter to IrTypeArgument.
fun IrMemberAccessExpression<*>.getTypeSubstitutionMap(
  irFunction: IrFunction
): Map<IrTypeParameterSymbol, IrTypeArgument> {
  val typeParameters = irFunction.allTypeParameters

  val superQualifierSymbol = (this as? IrCall)?.superQualifierSymbol

  val receiverType =
    if (superQualifierSymbol != null) superQualifierSymbol.defaultType as? IrSimpleType
    else getRealDispatcherReceiverForTypeSubstitution(irFunction)
  val dispatchReceiverTypeArguments = receiverType?.arguments ?: emptyList()

  if (typeParameters.isEmpty() && dispatchReceiverTypeArguments.isEmpty()) {
    return emptyMap()
  }

  var result = mutableMapOf<IrTypeParameterSymbol, IrTypeArgument>()
  if (dispatchReceiverTypeArguments.isNotEmpty()) {
    val parentTypeParameters =
      if (irFunction is IrConstructor) {
        val constructedClass = irFunction.parentAsClass
        if (!constructedClass.isInner && dispatchReceiver != null) {
          throw AssertionError(
            "Non-inner class constructor reference with dispatch receiver:\n${this.dump()}"
          )
        }
        extractTypeParameters(constructedClass.parent as IrClass)
      } else {
        extractTypeParameters(irFunction.parentClassOrNull!!)
      }
    // Modified to map to the actual IrTypeArgument.
    result =
      buildTypeSubstitutionMap(
        parentTypeParameters,
        dispatchReceiverTypeArguments,
        useDeclarationVariance = true,
      )
  }
  // End of modification

  return typeParameters.withIndex().associateTo(result) {
    // Modified to build a IrTypeArgument for member parameters.
    it.value.symbol to makeTypeProjection(getTypeArgument(it.index)!!, it.value.variance)
    // End of modification
  }
}

// TODO(b/377502016): remove this method when bug on JetBrain side is fixed.
private fun IrMemberAccessExpression<*>.getRealDispatcherReceiverForTypeSubstitution(
  callee: IrFunction
): IrSimpleType? {
  val dispatchReceiver = this.dispatchReceiver
  // With K2, we see extra implicit cast inserted on the dispatch receiver on fakeoverride function
  // calls.
  // The targeted function still being the fakeoverride defined on the original dispatch receiver
  // class (before the implicit cast), we need to return the original type to not lose any type
  // argument information that would break our type substitution mechanism.
  // Ex: let say we have a property `var foo : Foo<String> = ...`, when we call the
  // equals() methods on it, Kotlinc *may* implicitly cast `foo` to `Any` losing the type argument
  // of `Foo`. The call to `equals` is still targeting to the fakeoverride `equals` method defined
  // on Foo.

  // We try to narrow as much as we can the cases where we need to "patch" the dispatchReceiver type
  if (
    dispatchReceiver is IrTypeOperatorCall &&
      dispatchReceiver.operator == IrTypeOperator.IMPLICIT_CAST
  ) {
    val originalDispatchReceiver = dispatchReceiver.argument

    if (
      // We only need that for fake override where we need to propagate type parameter substitution.
      callee.isFakeOverride &&
        // Patch the dispatchReceiver only if the callee is defined on the class of the type before
        // the implicit cast
        callee.parentClassOrNull != null &&
        callee.parentClassOrNull!!.symbol == originalDispatchReceiver.type.classOrNull
    ) {

      return originalDispatchReceiver.type as? IrSimpleType
    }
  }

  return dispatchReceiver?.type as? IrSimpleType
}

// Adapted from org/jetbrains/kotlin/ir/util/IrUtils.kt
val IrFunctionReference.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrTypeArgument>
  get() = getTypeSubstitutionMap(symbol.owner)

val IrFunctionAccessExpression.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrTypeArgument>
  get() = getTypeSubstitutionMap(symbol.owner)

val IrType.typeSubstitutionMap: Map<IrTypeParameterSymbol, IrTypeArgument>
  get() =
    when (this) {
      is IrSimpleType -> this.getTypeSubstitutionMap(useDeclarationVariance = true)
      // TODO(b/226689222): Figure what to do for other subtypes of IrType, e.g. type variable with
      // bounds.
      else -> mapOf()
    }

fun IrSimpleType.getTypeSubstitutionMap(
  useDeclarationVariance: Boolean
): Map<IrTypeParameterSymbol, IrTypeArgument> {
  val typeParameters = classOrNull?.owner?.getAllTypeParameters()?.toList() ?: return mapOf()
  return buildTypeSubstitutionMap(typeParameters, arguments, useDeclarationVariance)
}

private fun buildTypeSubstitutionMap(
  typeParameters: List<IrTypeParameter>,
  typeArguments: List<IrTypeArgument>,
  useDeclarationVariance: Boolean,
): MutableMap<IrTypeParameterSymbol, IrTypeArgument> {
  val result = mutableMapOf<IrTypeParameterSymbol, IrTypeArgument>()
  for ((index, typeParam) in typeParameters.withIndex()) {
    if (index >= typeArguments.size) {
      // When local and anonymous classes that are declared in an inline function, they end up
      // seeing the outer parameters from the inlining context which are unrelated and not
      // referred inside the class; it is ok to not have them in the map.
      break
    }
    var typeArgument = typeArguments[index]
    if (
      useDeclarationVariance &&
        typeArgument is IrStarProjection &&
        typeParam.variance != Variance.IN_VARIANCE
    ) {
      // Star projections don't carry the information of the type bound from the declaration. Here
      // the star projection gets into its equivalent form 'out Bound'. Note that if the declaration
      // have variance 'in' then the star projection can be left alone.
      typeArgument = makeTypeProjection(typeParam.defaultType, Variance.OUT_VARIANCE)
    } else if (
      useDeclarationVariance &&
        typeArgument is IrTypeProjection &&
        typeParam.variance != Variance.INVARIANT &&
        // Only apply the variance from the declaration if it is not explicit in the usage
        typeArgument.variance == Variance.INVARIANT
    ) {
      typeArgument = makeTypeProjection(typeArgument.type, typeParam.variance)
    }
    result[typeParam.symbol] = typeArgument
  }

  return result
}

fun IrType.isArrayType(): Boolean = isArray() || isNullableArray() || isPrimitiveArray()

fun IrType.isClassType(fqName: FqNameUnsafe): Boolean =
  isClassType(fqName, false) || isClassType(fqName, true)

val IrClass.simpleSourceName: String?
  get() = if (NameUtils.hasName(name)) name.toString() else null

fun IrClass.findFunctionByName(name: String) = functions.single { it.name.asString() == name }

val IrClass.isCapturingEnclosingInstance: Boolean
  get() {
    // Never capture file classes.
    if (parentClassOrNull?.isFileClass == true) return false
    // Classes defined in a companion object never captures the companion. Any references to the
    // companion instance is replaced by a reference to the singleton field.
    if (parentClassOrNull?.isCompanion == true) return false
    // companion declarations annotated with JvmStatic may move in the enclosing class and be part
    // of a static function. Field initializers will be part of the static class initializer
    // function.
    if ((parent as? IrFunction)?.isStatic == true && (parent !is IrConstructor)) return false

    return isInner || (isLocal && parentClassOrNull != null)
  }

val IrClass.isFunctionalInterface: Boolean
  get() = singleAbstractMethod != null

val IrClass.singleAbstractMethod: IrFunction?
  get() {
    if (!isInterface) return null

    // For kotlin code we'll only consider a SAM if it's a fun interface.
    if (
      !isFromJava() &&
        !isFun &&
        // TODO(b/258302640): remove this hack when the interfaces in this package are defined as
        //  fun interfaces
        packageFqName != FqName("kotlin.jvm.functions")
    ) {
      return null
    }

    // For Kotlin this must be a fun interface so we know for certain that there's only a single
    // single abstract method.
    // For Java derived types we're relying upon the fake overrides to resolve all inherited methods
    // for us. That is, if C extends B extends A, then C will contain fake overrides for everything
    // defined on B and A. That allows us to simply check if C contains a SAM.
    return getSingleAbstractMethod()
  }

val IrClass.isAbstract: Boolean
  get() =
    when (modality) {
      Modality.ABSTRACT,
      Modality.SEALED -> true
      else -> false
    }

val IrClass.isFinal: Boolean
  get() = modality == Modality.FINAL

val IrClass.isFromSource: Boolean
  get() = source.containingFile != SourceFile.NO_SOURCE_FILE

fun IrDeclaration.getTopLevelEnclosingClass(): IrClass {
  if (this is IrClass && isTopLevel) {
    return this
  }

  // `parentClassOrNull` should not be null at this point as top level functions/properties have
  // been moved to a class.
  return checkNotNull(parentClassOrNull).getTopLevelEnclosingClass()
}

val IrClassifierSymbol.fqnOrFail: FqName
  get() =
    checkNotNull((owner as? IrDeclarationWithName)?.fqNameWhenAvailable) {
      "Fqn not available for this kind of class [$this]"
    }

val IrType.fqnOrFail: FqName
  get() = checkNotNull(classifierOrNull?.fqnOrFail) { "Fqn not available for this type [$this]" }

val IrFunction.isAbstract: Boolean
  get() = this is IrOverridableMember && modality == Modality.ABSTRACT

val IrFunction.isFinal: Boolean
  get() = this is IrOverridableMember && modality == Modality.FINAL

val IrDeclaration.isDeprecated: Boolean
  get() {
    return findAnnotation(KOTLIN_DEPRECATED_ANNOTATION_FQ_NAME) != null ||
      findAnnotation(JAVA_DEPRECATED_ANNOTATION_FQ_NAME) != null
  }

private val KOTLIN_DEPRECATED_ANNOTATION_FQ_NAME: FqName = FqName("kotlin.Deprecated")
private val JAVA_DEPRECATED_ANNOTATION_FQ_NAME: FqName = FqName("java.lang.Deprecated")

val IrValueParameter.isDoNotAutobox: Boolean
  get() = findAnnotation(DO_NOT_AUTOBOX_ANNOTATION_FQ_NAME) != null

private val DO_NOT_AUTOBOX_ANNOTATION_FQ_NAME: FqName = FqName(DO_NOT_AUTOBOX_ANNOTATION_NAME)

val IrFunction.isUncheckedCast: Boolean
  get() = findAnnotation(UNCHECKED_CAST_ANNOTATION_FQ_NAME) != null

private val UNCHECKED_CAST_ANNOTATION_FQ_NAME: FqName = FqName(UNCHECKED_CAST_ANNOTATION_NAME)

fun IrFunction.getWasmInfo(): String? = (this as IrDeclaration).getWasmInfo()

fun IrClass.getWasmInfo(): String? = (this as IrDeclaration).getWasmInfo()

private fun IrDeclaration.getWasmInfo(): String? =
  findAnnotation(WASM_ANNOTATION_FQ_NAME)
    ?.getValueArgumentAsConst(WASM_ANNOTATION_VALUE_NAME, IrConstKind.String)

private val WASM_ANNOTATION_FQ_NAME: FqName = FqName(WASM_ANNOTATION_NAME)
private val WASM_ANNOTATION_VALUE_NAME = Name.identifier("value")

private fun IrDeclaration.findAnnotation(name: FqName): IrConstructorCall? {
  val annotation = getAnnotation(name)
  if (annotation != null) return annotation
  return when (this) {
    is IrSimpleFunction -> correspondingPropertySymbol?.owner?.findAnnotation(name)
    is IrField -> correspondingPropertySymbol?.owner?.findAnnotation(name)
    else -> null
  }
}

fun <T> IrConstructorCall.getValueArgumentAsConst(name: Name, kind: IrConstKind<T>): T? {
  val valueArgumentAsConst = getValueArgument(name) as? IrConst<*> ?: return null
  return kind.valueOf(valueArgumentAsConst)
}

val IrDeclaration.isSynthetic
  get() =
    isFakeOverride ||
      // Enum synthetic functions values and valueOf will be generated by later passes.
      origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER ||
      // Lowered properties/typealiases with annotations get a synthetic stub function added to hold
      // onto the original annotations. We have no use for these stubs.
      origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS

private val clinitName = Name.special("<clinit>")

val IrDeclaration.isClinit: Boolean
  get() = this is IrFunction && name == clinitName

private val IrFunction.isPropertyGetter: Boolean
  get() = this is IrSimpleFunction && this == this.correspondingPropertySymbol?.owner?.getter

val IrFunction.hasVoidReturn: Boolean
  get() = returnType.isUnit() && !isPropertyGetter

fun IrFunction.javaName(jvmBackendContext: JvmBackendContext): String? =
  when (this) {
    is IrConstructor -> null
    is IrSimpleFunction -> this.javaName(jvmBackendContext)
    else -> name.asString()
  }

fun IrSimpleFunction.javaName(jvmBackendContext: JvmBackendContext): String {
  // Always prefer the @JvmName, if present.
  getJvmNameFromAnnotation()?.let {
    return sanitizeJvmName(it)
  }

  // If this function is a builtin function using a different name on the jvm or an override of
  // such a function, use the jvm name.
  getBuiltinFunctionJvmName(this.symbol)?.let {
    return it
  }

  // TODO(b/317553886): Signature mangling should be applied during inline class lowering.
  if (signatureRequiresMangling) {
    return InlineClassAbi.mangledNameFor(
        jvmBackendContext,
        this,
        mangleReturnTypes = true,
        useOldMangleRules = false,
      )
      .asJavaName()
  }
  val correspondingProperty = correspondingPropertySymbol?.owner ?: return name.asJavaName()
  val propertyName =
    CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, correspondingProperty.name.asJavaName())
  return if (valueParameters.isEmpty()) "get$propertyName" else "set$propertyName"
}

/**
 * Returns the name to use on the jvm for builtin functions and their overrides.
 *
 * Some builtin functions are mapped to jvm methods with a different name. We need to use the name
 * used on jvm when transpiling these functions and their overrides to keep Java interoperability
 * working.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun getBuiltinFunctionJvmName(irFunctionSymbol: IrFunctionSymbol): String? {
  val descriptor = irFunctionSymbol.descriptor as? CallableMemberDescriptor ?: return null

  // if this method is directly mapped to a method with a different name, return this name
  getJvmMethodNameIfSpecial(descriptor)?.let { jvmName ->
    return jvmName
  }

  // Look if this is an override of a builtin function mapped to method with a different name. In
  // this case returns the jvm name of the overridden function.
  descriptor.getOverriddenBuiltinWithDifferentJvmName()?.let {
    return getJvmMethodNameIfSpecial(it)
  }

  return null
}

/**
 * Returns `true` if the function reference is a reference to a synthetic adapter function created
 * by Kotlin compiler. E.g: function with varargs can be used in a context of a function type
 * without varargs. In this case, Kotlin compiler creates an extra function adapter and reference to
 * the original function in that context is seen as a reference to the adapter function.
 */
val IrFunctionReference.isAdaptedFunctionReference: Boolean
  get() = origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE

val IrValueDeclaration.javaName: String
  get() = NameUtils.sanitizeAsJavaIdentifier(name.asStringStripSpecialMarkers())

private fun Name.asJavaName() = sanitizeJvmName(asStringStripSpecialMarkers())

// TODO(b/228454104): There are many more characters that kotlin allows that need to be sanitized.
//  We should also give IrValueDeclarations the same treatment.
private fun sanitizeJvmName(name: String) = name.replace('-', '$')

val IrExpression.isUnitInstanceReference: Boolean
  // There is only one object instance field of type Unit, it's the unique instance of Unit.
  get() =
    type.isUnit() &&
      this is IrGetField &&
      symbol.owner.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE

val IrSimpleFunction.signatureRequiresMangling
  get() =
    (fullValueParameterList.any { it.type.getRequiresMangling() } ||
      returnType.getRequiresMangling()) &&
      // TODO(b/228143153): Bridge methods should be created for overrides as part of lowering.
      !isOverridableOrOverrides

fun IrBreakContinue.resolveLabel(): String? = label ?: loop.label

// TODO(b/259156400): Remove when the original stdlib file compiles with `-Xserialize-ir` flag.
fun IrClass.isStubbedPrimitiveRangeClass(): Boolean {
  if (packageFqName != FqName("kotlin.internal.j2cl")) {
    return false
  }
  val enclosingClass = if (isCompanion) parentAsClass else this
  return when (enclosingClass.name.asString()) {
    "CharRange",
    "IntRange",
    "LongRange" -> true
    else -> false
  }
}

// TODO(b/259156400): Remove when the original stdlib file compiles with `-Xserialize-ir` flag.
fun IrClass.isStubbedPrimitiveIteratorClass(): Boolean {
  if (packageFqName != FqName("kotlin.internal.j2cl")) {
    return false
  }
  val enclosingClass = if (isCompanion) parentAsClass else this
  return when (enclosingClass.name.asString()) {
    "ByteIterator",
    "CharIterator",
    "ShortIterator",
    "IntIterator",
    "LongIterator",
    "FloatIterator",
    "DoubleIterator",
    "BooleanIterator" -> true
    else -> false
  }
}

internal val IrDeclaration.isCompanionMember: Boolean
  get() = (parent as? IrClass)?.isCompanion == true

fun IrElement.getNameSourcePosition(irFile: IrFile, name: String? = null): SourcePosition =
  getNamedPsiElement(irFile)?.getSourcePosition(irFile, name) ?: SourcePosition.NONE

fun IrElement.getSourcePosition(irFile: IrFile): SourcePosition {
  var sourceElementIr = this

  if (this is IrField && origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE) {
    // For companion instance fields, we map this field to the corresponding object because its
    // corresponding ir element does not exist in its original source.
    sourceElementIr = type.classOrFail.owner
  }

  return sourceElementIr.getPsiElement(irFile)?.getSourcePosition(irFile) ?: SourcePosition.NONE
}

val IrElement.isTemporaryVariable: Boolean
  get() = this is IrVariable && origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE

val IrElement.isPrefixExpression: Boolean
  get() =
    this is IrSetValue &&
      (origin == IrStatementOrigin.PREFIX_INCR || origin == IrStatementOrigin.PREFIX_DECR)

val IrElement.isPostfixExpression: Boolean
  get() =
    this is IrSetValue &&
      (origin == IrStatementOrigin.POSTFIX_INCR || origin == IrStatementOrigin.POSTFIX_DECR)

val IrElement.isAugmentedAssignement: Boolean
  get() =
    this is IrSetValue &&
      (origin == IrStatementOrigin.PLUSEQ ||
        origin == IrStatementOrigin.MINUSEQ ||
        origin == IrStatementOrigin.MULTEQ ||
        origin == IrStatementOrigin.DIVEQ ||
        origin == IrStatementOrigin.PERCEQ)

val IrElement.isVariableDeclaration: Boolean
  get() = this is IrVariable && origin == IrDeclarationOrigin.DEFINED

val IrElement.isFieldDeclaration: Boolean
  get() = this is IrSetField && origin == IrStatementOrigin.INITIALIZE_FIELD

val IrElement.isFieldAssignment: Boolean
  get() = this is IrSetField && !isFieldDeclaration

val IrElement.isLabeledExpression: Boolean
  get() = this is IrLoop && label != null

val IrElement.isPrimaryConstructor: Boolean
  get() = this is IrConstructor && this.isPrimary

fun IrAnnotationContainer.copyAnnotationsWhen(
  filter: IrConstructorCall.() -> Boolean
): List<IrConstructorCall> {
  return annotations.mapNotNull {
    if (it.filter()) it.deepCopyWithSymbols(this as? IrDeclarationParent) else null
  }
}

val IrProperty.hasAccessors: Boolean
  get() = visibility != DescriptorVisibilities.PRIVATE || hasDefinedAccessors

private val IrProperty.hasDefinedAccessors: Boolean
  get() =
    (getter != null && !getter!!.isDefaultPropertyAccessor) ||
      (setter != null && !setter!!.isDefaultPropertyAccessor)

private val IrFunction.isDefaultPropertyAccessor: Boolean
  get() =
    origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
      (this is IrSimpleFunction && correspondingPropertySymbol != null)
