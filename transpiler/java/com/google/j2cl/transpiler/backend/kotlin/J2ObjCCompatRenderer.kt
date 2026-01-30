/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.AstUtils.createImplicitConstructorDescriptor
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.Member
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isBoxedType
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.companionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.declaration
import com.google.j2cl.transpiler.backend.kotlin.ast.toCompanionObjectOrNull
import com.google.j2cl.transpiler.backend.kotlin.common.backslashEscapedString
import com.google.j2cl.transpiler.backend.kotlin.common.inSingleQuotes
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.orIfNull
import com.google.j2cl.transpiler.backend.kotlin.common.runIf
import com.google.j2cl.transpiler.backend.kotlin.common.titleCased
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.dependent
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.flatten
import com.google.j2cl.transpiler.backend.kotlin.objc.className
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.objc.defineAlias
import com.google.j2cl.transpiler.backend.kotlin.objc.dependentFloatSource
import com.google.j2cl.transpiler.backend.kotlin.objc.dependentMathSource
import com.google.j2cl.transpiler.backend.kotlin.objc.dependentNsObjCRuntimeSource
import com.google.j2cl.transpiler.backend.kotlin.objc.dependentSource
import com.google.j2cl.transpiler.backend.kotlin.objc.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.objc.expressionStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.functionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.objc.getProperty
import com.google.j2cl.transpiler.backend.kotlin.objc.id
import com.google.j2cl.transpiler.backend.kotlin.objc.macroDefine
import com.google.j2cl.transpiler.backend.kotlin.objc.methodCall
import com.google.j2cl.transpiler.backend.kotlin.objc.nsAssumeNonnull
import com.google.j2cl.transpiler.backend.kotlin.objc.nsCopying
import com.google.j2cl.transpiler.backend.kotlin.objc.nsInline
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableArray
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableDictionary
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableSet
import com.google.j2cl.transpiler.backend.kotlin.objc.nsNumber
import com.google.j2cl.transpiler.backend.kotlin.objc.nsObject
import com.google.j2cl.transpiler.backend.kotlin.objc.nsString
import com.google.j2cl.transpiler.backend.kotlin.objc.plusAssignment
import com.google.j2cl.transpiler.backend.kotlin.objc.plusSemicolon
import com.google.j2cl.transpiler.backend.kotlin.objc.protocolName
import com.google.j2cl.transpiler.backend.kotlin.objc.returnStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.sourceWithDependencies
import com.google.j2cl.transpiler.backend.kotlin.objc.toNullable
import com.google.j2cl.transpiler.backend.kotlin.objc.toPointer
import com.google.j2cl.transpiler.backend.kotlin.objc.typedef
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated

// TODO(b/442834826): Refactor to use type model instead of AST nodes.
internal class J2ObjCCompatRenderer(private val objCNamePrefix: String) {
  internal fun source(compilationUnit: CompilationUnit): Source =
    dependenciesAndDeclarationsSource(compilationUnit).ifNotEmpty {
      emptyLineSeparated(fileCommentSource(compilationUnit), it) + Source.NEW_LINE
    }

  private fun fileCommentSource(compilationUnit: CompilationUnit): Source =
    comment(source("Generated by J2KT from \"${compilationUnit.packageRelativePath}\""))

  private fun dependenciesAndDeclarationsSource(compilationUnit: CompilationUnit): Source =
    declarationsDependentSource(compilationUnit).sourceWithDependencies

  private fun declarationsDependentSource(compilationUnit: CompilationUnit): Dependent<Source> =
    nsAssumeNonnull(
      declarationsDependentSources(compilationUnit).flatten().map { emptyLineSeparated(it) }
    )

  private fun declarationsDependentSources(
    compilationUnit: CompilationUnit
  ): List<Dependent<Source>> =
    includedTypes(compilationUnit).flatMap(::declarationsDependentSources)

  private fun includedTypes(compilationUnit: CompilationUnit): List<Type> =
    compilationUnit.types.filter(::shouldRender).flatMap { listOf(it) + includedTypes(it) }

  private fun includedTypes(type: Type): List<Type> =
    type.types.filter(::shouldRender).flatMap { listOf(it) + includedTypes(it) }

  private fun shouldRender(type: Type): Boolean =
    !nameIsMappedInObjC(type.declaration) && shouldRender(type.declaration)

  private fun declarationsDependentSources(type: Type): List<Dependent<Source>> = buildList {
    if (objCNamePrefix.isNotEmpty()) {
      add(aliasDeclarationDependentSource(type.declaration))
      type.toCompanionObjectOrNull()?.let { add(aliasDeclarationDependentSource(it.declaration)) }
    }

    if (type.isEnum) {
      add(nsEnumTypedefDependentSource(type))
    }

    addAll(type.fields.map(::fieldConstantDefineDependentSource))

    addAll(type.members.flatMap(::functionDependentSources))

    // Render implicit constructor
    if (!type.isInterface && type.constructors.isEmpty()) {
      addAll(
        functionDependentSources(
          Method.newBuilder()
            .setMethodDescriptor(createImplicitConstructorDescriptor(type.typeDescriptor))
            .setSourcePosition(type.sourcePosition)
            .build()
        )
      )
    }
  }

  private fun aliasDeclarationDependentSource(typeDeclaration: TypeDeclaration): Dependent<Source> =
    objCNameDependentSource(typeDeclaration).map { objCName ->
      defineAlias(source(objCAlias(typeDeclaration)), objCName)
    }

  private fun aliasDeclarationDependentSource(
    companionDeclaration: CompanionDeclaration
  ): Dependent<Source> =
    objCNameDependentSource(companionDeclaration).map { objCName ->
      defineAlias(source(objCAlias(companionDeclaration)), objCName)
    }

  private fun objCAlias(typeDeclaration: TypeDeclaration): String =
    typeDeclaration.objCNameWithoutPrefix

  private fun objCAlias(companionDeclaration: CompanionDeclaration): String =
    companionDeclaration.objCNameWithoutPrefix

  // We append "_" to the enum type name because the @ObjcEnum does not insert an underscore
  // between the type name and the literal name. Hence we use a typedef to remove it again.
  private fun nsEnumTypedefDependentSource(type: Type): Dependent<Source> =
    objCEnumName(type.declaration).let { enumName ->
      typedef(enumName + "_", dependentSource(enumName))
    }

  private fun propertyQualifierDependentSource(
    fieldDescriptor: FieldDescriptor
  ): Dependent<Source> =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.let { typeDeclaration ->
      if (fieldDescriptor.isStatic && !fieldDescriptor.isEnumConstant) {
        sharedDependentSource(typeDeclaration.companionDeclaration)
      } else {
        objCNameDependentSource(typeDeclaration)
      }
    }

  private fun getPropertyObjCName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.objCName

  private fun fieldGetFunctionDependentSource(field: Field): Dependent<Source> =
    field.descriptor.takeIf(::shouldRender)?.let(::getFunctionDependentSource)
      ?: dependent(Source.EMPTY)

  private fun getFunctionDependentSource(fieldDescriptor: FieldDescriptor): Dependent<Source> =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = objCDependentSource(fieldDescriptor.typeDescriptor),
      name = getFunctionName(fieldDescriptor),
      statements = listOf(returnStatement(getExpressionDependentSource(fieldDescriptor))),
    )

  private fun getFunctionName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix +
      "_get_" +
      fieldDescriptor.name!!.objCName

  private fun getExpressionDependentSource(fieldDescriptor: FieldDescriptor): Dependent<Source> =
    if (fieldDescriptor.typeDescriptor.isPrimitive && fieldDescriptor.isCompileTimeConstant) {
      dependentSource(defineConstantName(fieldDescriptor))
    } else {
      propertyQualifierDependentSource(fieldDescriptor).map {
        dotSeparated(it, source(getPropertyObjCName(fieldDescriptor)))
      }
    }

  private fun setPropertyObjCName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.objCName.escapeObjCKeyword

  private fun fieldSetFunctionDependentSource(field: Field): Dependent<Source> =
    field.descriptor.takeIf { !it.isFinal && shouldRender(it) }?.let(::setFunctionDependentSource)
      ?: dependent(Source.EMPTY)

  private fun setFunctionDependentSource(fieldDescriptor: FieldDescriptor): Dependent<Source> =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = objCDependentSource(PrimitiveTypes.VOID),
      name = setFunctionName(fieldDescriptor),
      parameters = listOf(setParameterDependentSource(fieldDescriptor)),
      statements = listOf(setStatementDependentSource(fieldDescriptor)),
    )

  private fun setFunctionName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix +
      "_set_" +
      fieldDescriptor.name!!.objCName

  private fun setStatementDependentSource(fieldDescriptor: FieldDescriptor): Dependent<Source> =
    dotSeparated(
        propertyQualifierDependentSource(fieldDescriptor),
        dependentSource(setPropertyObjCName(fieldDescriptor)),
      )
      .plusAssignment(dependentSource(setFunctionParameterName))
      .plusSemicolon()

  private fun setParameterDependentSource(fieldDescriptor: FieldDescriptor): Dependent<Source> =
    objCDependentSource(fieldDescriptor.typeDescriptor).map {
      spaceSeparated(it, source(setFunctionParameterName))
    }

  private val setFunctionParameterName: String
    get() = "value"

  private fun fieldConstantDefineDependentSource(field: Field): Dependent<Source> =
    field.descriptor.takeIf(::shouldRender)?.let(::constantDefineDependentSource)
      ?: dependent(Source.EMPTY)

  private fun constantDefineDependentSource(fieldDescriptor: FieldDescriptor): Dependent<Source>? =
    fieldDescriptor.constantValue?.let(::constantValueDependentSource)?.map { literalSource ->
      macroDefine(spaceSeparated(source(defineConstantName(fieldDescriptor)), literalSource))
    }

  private fun defineConstantName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix +
      "_" +
      fieldDescriptor.name!!

  private fun functionDependentSources(member: Member): List<Dependent<Source>> =
    when (member) {
      is Method -> methodFunctionDependentSources(member)
      is Field ->
        listOf(fieldGetFunctionDependentSource(member), fieldSetFunctionDependentSource(member))
      else -> emptyList()
    }

  private fun methodFunctionDependentSources(method: Method): List<Dependent<Source>> =
    method
      .takeIf { shouldRender(it.descriptor) }
      ?.toObjCNames()
      ?.let { functionDependentSources(method, it) } ?: listOf()

  private fun shouldRender(methodDescriptor: MethodDescriptor): Boolean =
    !methodDescriptor.hasAnnotation("com.google.j2kt.annotations.HiddenFromObjC") &&
      methodDescriptor.visibility.isPublic &&
      when {
        // Static methods are always rendered.
        methodDescriptor.isStatic -> true
        // Constructors are rendered except for specific cases.
        methodDescriptor.isConstructor ->
          when {
            // Inner class constructors have implicit `outer` parameter.
            methodDescriptor.enclosingTypeDescriptor.typeDeclaration.isKtInner -> false
            // Primitive constructors are not rendered, as they are currently implemented in Kotlin
            // as `invoke` calls and are deprecated in Java.
            isBoxedType(methodDescriptor.enclosingTypeDescriptor) -> false
            else -> true
          }
        else -> false
      } &&
      shouldRender(methodDescriptor.returnTypeDescriptor) &&
      methodDescriptor.parameterTypeDescriptors.all(::shouldRender) &&
      canInferObjCName(methodDescriptor) &&
      !methodDescriptor.ktInfo.isThrows

  private fun canInferObjCName(methodDescriptor: MethodDescriptor): Boolean =
    methodDescriptor.parameterTypeDescriptors.isEmpty() ||
      methodDescriptor.objectiveCName.let { it != null && it.endsWith(":") } ||
      methodDescriptor.parameterTypeDescriptors.all { !it.isProtobuf }

  private fun shouldRender(fieldDescriptor: FieldDescriptor): Boolean =
    !fieldDescriptor.hasAnnotation("com.google.j2kt.annotations.HiddenFromObjC") &&
      fieldDescriptor.visibility.isPublic &&
      (fieldDescriptor.isStatic || fieldDescriptor.enclosingTypeDescriptor.isInterface) &&
      shouldRender(fieldDescriptor.typeDescriptor)

  private fun shouldRender(typeDescriptor: TypeDescriptor): Boolean =
    when (typeDescriptor) {
      is PrimitiveTypeDescriptor -> true
      is DeclaredTypeDescriptor ->
        shouldRenderDescriptor(typeDescriptor.typeDeclaration) &&
          (!isBoxedType(typeDescriptor) || typeDescriptor.isNullable)
      is ArrayTypeDescriptor -> false
      is TypeVariable -> shouldRender(typeDescriptor.upperBoundTypeDescriptor)
      is IntersectionTypeDescriptor -> shouldRender(typeDescriptor.firstType)
      is UnionTypeDescriptor -> false
    }

  private fun shouldRender(typeDeclaration: TypeDeclaration): Boolean =
    shouldRenderDescriptor(typeDeclaration) &&
      !typeDeclaration.isProtobuf &&
      !typeDeclaration.isAnnotation

  private fun shouldRenderDescriptor(typeDeclaration: TypeDeclaration): Boolean =
    typeDeclaration.visibility.isPublic &&
      existsInObjC(typeDeclaration) &&
      !typeDeclaration.toDescriptor().isCollection

  private fun existsInObjC(typeDeclaration: TypeDeclaration): Boolean =
    !typeDeclaration.isKtNative ||
      !typeDeclaration.isFromJRE() ||
      !KT_NATIVE_JRE_EXCLUDE.any { typeDeclaration.qualifiedSourceName.startsWith(it) } ||
      mappedObjCNameDependentSource(typeDeclaration) != null

  private fun functionDependentSources(
    method: Method,
    objCNames: MethodObjCNames,
  ): List<Dependent<Source>> =
    if (method.isConstructor) {
      listOf(
        functionDependentSource(method, objCNames, prefix = "create_"),
        functionDependentSource(method, objCNames, prefix = "new_"),
      )
    } else {
      listOf(functionDependentSource(method, objCNames))
    }

  private fun functionDependentSource(
    method: Method,
    objCNames: MethodObjCNames,
    prefix: String = "",
  ): Dependent<Source> =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = objCDependentSource(method.descriptor.returnTypeDescriptor),
      name = functionName(method.descriptor, objCNames, prefix),
      parameters = method.parameters.map(::variableDependentSource),
      statements =
        statementDependentSources(method, objCNames.escapeObjCMethod(method.isConstructor)),
    )

  private fun functionName(
    methodDescriptor: MethodDescriptor,
    objCNames: MethodObjCNames,
    prefix: String = "",
  ): String =
    methodDescriptor.enclosingTypeDescriptor
      .objCName(useId = true)
      .let { "$prefix$it" }
      .plus("_")
      .plus(functionBaseName(methodDescriptor, objCNames))
      .runIf(functionNameContainsParameterNames(methodDescriptor)) {
        plus(functionParameterNames(methodDescriptor, objCNames))
      }

  private fun functionBaseName(
    methodDescriptor: MethodDescriptor,
    objCNames: MethodObjCNames,
  ): String =
    if (methodDescriptor.isConstructor && !functionNameContainsParameterNames(methodDescriptor)) {
      methodDescriptor.objectiveCName ?: objCNames.objCName.string
    } else {
      objCNames.objCName.string
    }

  private fun functionNameContainsParameterNames(methodDescriptor: MethodDescriptor): Boolean =
    methodDescriptor.objectiveCName.let { it == null || it.contains(":") }

  private fun functionParameterNames(
    methodDescriptor: MethodDescriptor,
    objCNames: MethodObjCNames,
  ) =
    objCNames.parameterObjCNames
      .mapIndexed { index, name ->
        name.string
          .letIf(index == 0) { it.titleCased.letIf(methodDescriptor.isConstructor) { "With$it" } }
          .plus("_")
      }
      .joinToString("")

  private fun statementDependentSources(
    method: Method,
    objCNames: MethodObjCNames,
  ): List<Dependent<Source>> =
    if (isPrimitiveVoid(method.descriptor.returnTypeDescriptor)) {

      listOf(expressionStatement(methodCallDependentSource(method, objCNames)))
    } else {
      listOf(returnStatement(methodCallDependentSource(method, objCNames)))
    }

  private fun methodCallDependentSource(
    method: Method,
    objCNames: MethodObjCNames,
  ): Dependent<Source> =
    methodCall(
      target = methodCallTargetDependentSource(method),
      name = objCSelector(objCNames),
      arguments = method.parameters.map(::nameDependentSource),
    )

  private fun methodCallTargetDependentSource(method: Method): Dependent<Source> =
    if (method.isConstructor) {
      allocDependentSource(method.descriptor.enclosingTypeDescriptor.typeDeclaration)
    } else {
      sharedDependentSource(
        method.descriptor.enclosingTypeDescriptor.typeDeclaration.companionDeclaration
      )
    }

  private fun objCSelector(methodObjCNames: MethodObjCNames): String =
    methodObjCNames.objCName.string.plus(
      methodObjCNames.parameterObjCNames
        .mapIndexed { index, name -> name.string.letIf(index == 0) { it.titleCased } + ":" }
        .joinToString("")
    )

  private fun variableDependentSource(variable: Variable): Dependent<Source> =
    combine(objCDependentSource(variable.typeDescriptor), nameDependentSource(variable)) {
      typeSource,
      nameSource ->
      spaceSeparated(typeSource, nameSource)
    }

  private fun nameDependentSource(variable: Variable): Dependent<Source> =
    dependentSource(variable.name.objCName.escapeObjCKeyword.escapeJ2ObjCKeyword)

  private fun objCNameDependentSource(
    companionDeclaration: CompanionDeclaration
  ): Dependent<Source> =
    mappedKtNativeCompanionDependentSource(companionDeclaration.enclosingTypeDeclaration)
      ?: className(companionDeclaration.objCName(objCNamePrefix))

  private fun sharedDependentSource(companionDeclaration: CompanionDeclaration): Dependent<Source> =
    getProperty(objCNameDependentSource(companionDeclaration), "shared")

  private fun allocDependentSource(typeDeclaration: TypeDeclaration): Dependent<Source> =
    (mappedKtNativeBridgeDependentSource(typeDeclaration)
        ?: objCNameDependentSource(typeDeclaration))
      .map { inSquareBrackets(spaceSeparated(it, source("alloc"))) }

  private fun objCNameDependentSource(typeDeclaration: TypeDeclaration): Dependent<Source> =
    mappedObjCNameDependentSource(typeDeclaration)
      ?: mappedKtNativeDependentSource(typeDeclaration)
      ?: nonMappedObjCNameDependentSource(typeDeclaration)

  private fun nameIsMappedInObjC(typeDeclaration: TypeDeclaration): Boolean =
    mappedObjCNameDependentSource(typeDeclaration) != null

  private fun mappedObjCNameDependentSource(typeDeclaration: TypeDeclaration): Dependent<Source>? =
    when (typeDeclaration.qualifiedBinaryName) {
      "java.lang.Object" -> nsObject
      "java.lang.String" -> nsString
      "java.lang.Number" -> nsNumber
      "java.lang.Cloneable" -> nsCopying
      "java.util.List" -> nsMutableArray
      "java.util.Set" -> nsMutableSet
      "java.util.Map" -> nsMutableDictionary
      else -> null
    }

  private fun ktNativeNameObjCNamePrefix(name: String): String =
    if (name.startsWith("kotlin.")) "GKOT" else objCNamePrefix

  private fun ktNativeNameToObjCName(name: String): String =
    ktNativeNameObjCNamePrefix(name) + ktNativeNameObjCNameWithoutPrefix(name)

  private fun ktNativeNameObjCNameWithoutPrefix(name: String): String =
    ktNativeNameObjCPackagePrefix(name) + ktNativeNameObjCSimpleName(name)

  private fun ktNativeNameObjCPackagePrefix(name: String): String =
    when {
      ktNativeNameUsesSimpleObjCName(name) -> ""
      name.startsWith("kotlin.") -> "Kotlin"
      else -> name.substring(0, name.lastIndexOf('.')).objCPackagePrefix
    }

  private fun ktNativeNameObjCSimpleName(name: String): String =
    name.substring(name.lastIndexOf('.') + 1)

  private fun ktNativeNameUsesSimpleObjCName(name: String): Boolean =
    KT_NATIVE_SIMPLE_OBJC_NAME_TYPES.contains(name)

  private fun mappedKtNativeDependentSource(
    kind: TypeDeclaration.Kind,
    name: String,
  ): Dependent<Source>? = objCNameDependentSource(kind, ktNativeNameToObjCName(name))

  private fun mappedKtNativeDependentSource(typeDeclaration: TypeDeclaration): Dependent<Source>? =
    typeDeclaration.ktNativeQualifiedName?.let {
      mappedKtNativeDependentSource(typeDeclaration.kind, it)
    }

  private fun mappedKtNativeBridgeDependentSource(
    typeDeclaration: TypeDeclaration
  ): Dependent<Source>? =
    KT_NATIVE_BRIDGE_NAME_MAP.get(
        typeDeclaration.ktBridgeQualifiedName ?: typeDeclaration.ktNativeQualifiedName
      )
      .orIfNull { typeDeclaration.ktBridgeQualifiedName }
      .orIfNull { typeDeclaration.ktNativeQualifiedName }
      ?.let { mappedKtNativeDependentSource(typeDeclaration.kind, it) }

  private fun mappedKtNativeCompanionDependentSource(
    typeDeclaration: TypeDeclaration
  ): Dependent<Source>? =
    typeDeclaration.ktCompanionQualifiedName
      .orIfNull { typeDeclaration.ktNativeQualifiedName }
      ?.let { mappedKtNativeDependentSource(typeDeclaration.kind, it + "Companion") }

  private fun nonMappedObjCNameDependentSource(
    typeDeclaration: TypeDeclaration
  ): Dependent<Source> =
    objCNameDependentSource(typeDeclaration.kind, typeDeclaration.objCName(prefix = objCNamePrefix))

  private fun objCNameDependentSource(kind: TypeDeclaration.Kind, name: String): Dependent<Source> =
    when (kind) {
      TypeDeclaration.Kind.CLASS,
      TypeDeclaration.Kind.ENUM -> className(name)
      TypeDeclaration.Kind.INTERFACE -> protocolName(name)
    }

  private fun objCDependentSource(typeDescriptor: TypeDescriptor): Dependent<Source> =
    when {
      typeDescriptor is PrimitiveTypeDescriptor -> primitiveObjCDependentSource(typeDescriptor)
      typeDescriptor is DeclaredTypeDescriptor && !typeDescriptor.typeDeclaration.isProtobuf ->
        declaredObjCDependentSource(typeDescriptor)
      // TODO: Handle TypeVariable and Array
      else -> id
    }.runIf(typeDescriptor.canBeNull()) { toNullable() }

  private fun primitiveObjCDependentSource(
    primitiveTypeDescriptor: PrimitiveTypeDescriptor
  ): Dependent<Source> =
    when (primitiveTypeDescriptor) {
      PrimitiveTypes.VOID -> dependentSource("void")
      PrimitiveTypes.BOOLEAN -> booleanTypeDependentSource
      PrimitiveTypes.CHAR -> charTypeDependentSource
      PrimitiveTypes.BYTE -> byteTypeDependentSource
      PrimitiveTypes.SHORT -> shortTypeDependentSource
      PrimitiveTypes.INT -> intTypeDependentSource
      PrimitiveTypes.LONG -> longTypeDependentSource
      PrimitiveTypes.FLOAT -> floatTypeDependentSource
      PrimitiveTypes.DOUBLE -> doubleTypeDependentSource
      else -> error("$this.primitiveObjCDependentSource")
    }

  private fun declaredObjCDependentSource(
    declaredTypeDescriptor: DeclaredTypeDescriptor
  ): Dependent<Source> =
    when {
      isJavaLangObject(declaredTypeDescriptor) -> id
      declaredTypeDescriptor.isInterface -> interfaceObjCDependentSource(declaredTypeDescriptor)
      else -> objCNameDependentSource(declaredTypeDescriptor.typeDeclaration).toPointer()
    }

  private fun interfaceObjCDependentSource(
    declaredTypeDescriptor: DeclaredTypeDescriptor
  ): Dependent<Source> =
    combine(id, objCNameDependentSource(declaredTypeDescriptor.typeDeclaration)) {
      idSource,
      typeSource ->
      join(idSource, inAngleBrackets(typeSource))
    }

  private val booleanTypeDependentSource: Dependent<Source>
    get() = dependentNsObjCRuntimeSource("BOOL")

  private val charTypeDependentSource: Dependent<Source>
    get() = dependentNsObjCRuntimeSource("uint16_t")

  private val byteTypeDependentSource: Dependent<Source>
    get() = dependentNsObjCRuntimeSource("int8_t")

  private val shortTypeDependentSource: Dependent<Source>
    get() = dependentNsObjCRuntimeSource("int16_t")

  private val intTypeDependentSource: Dependent<Source>
    get() = dependentNsObjCRuntimeSource("int32_t")

  private val longTypeDependentSource: Dependent<Source>
    get() = dependentNsObjCRuntimeSource("int64_t")

  private val floatTypeDependentSource: Dependent<Source>
    get() = dependentSource("float")

  private val doubleTypeDependentSource: Dependent<Source>
    get() = dependentSource("double")

  private fun objCEnumName(typeDeclaration: TypeDeclaration): String =
    "${typeDeclaration.objCNameWithoutPrefix}_Enum"

  private fun constantValueDependentSource(literal: Literal): Dependent<Source>? =
    when (literal) {
      is BooleanLiteral -> booleanLiteralDependentSource(literal)
      is NumberLiteral -> numberLiteralDependentSource(literal)
      else -> null
    }

  // Literal rendering code is based on:
  // /google3/third_party/java_src/j2objc/translator/src/main/java/com/google/devtools/j2objc/gen/LiteralGenerator.java

  private fun booleanLiteralDependentSource(booleanLiteral: BooleanLiteral): Dependent<Source>? =
    literalDependentSource(booleanLiteral.value)

  private fun numberLiteralDependentSource(numberLiteral: NumberLiteral): Dependent<Source>? =
    when (numberLiteral.typeDescriptor) {
      PrimitiveTypes.CHAR -> literalDependentSource(numberLiteral.value.toInt().toChar())
      PrimitiveTypes.BYTE -> literalDependentSource(numberLiteral.value.toByte())
      PrimitiveTypes.SHORT -> literalDependentSource(numberLiteral.value.toShort())
      PrimitiveTypes.INT -> literalDependentSource(numberLiteral.value.toInt())
      PrimitiveTypes.LONG -> literalDependentSource(numberLiteral.value.toLong())
      PrimitiveTypes.FLOAT -> literalDependentSource(numberLiteral.value.toFloat())
      PrimitiveTypes.DOUBLE -> literalDependentSource(numberLiteral.value.toDouble())
      else -> null
    }

  private fun literalDependentSource(boolean: Boolean): Dependent<Source> =
    dependentNsObjCRuntimeSource(if (boolean) "true" else "false")

  private fun literalDependentSource(char: Char): Dependent<Source> =
    dependentSource(
      char.code.let { code ->
        if (code in 0x20..0x7E) {
          when (char) {
            '\'',
            '\\' -> char.backslashEscapedString
            else -> char.toString()
          }.inSingleQuotes
        } else {
          String.format("0x%04x", code)
        }
      }
    )

  private fun literalDependentSource(byte: Byte): Dependent<Source> = dependentSource("$byte")

  private fun literalDependentSource(short: Short): Dependent<Source> = dependentSource("$short")

  private fun literalDependentSource(int: Int): Dependent<Source> =
    when (int) {
      Int.MIN_VALUE ->
        intTypeDependentSource.map {
          inParentheses(spaceSeparated(inParentheses(it), source("0x80000000")))
        }
      else -> dependentSource("$int")
    }

  private fun literalDependentSource(long: Long): Dependent<Source> =
    when (long) {
      Long.MIN_VALUE ->
        longTypeDependentSource.map {
          inParentheses(spaceSeparated(inParentheses(it), source("0x8000000000000000LL")))
        }
      else -> dependentSource("${long}LL")
    }

  private fun literalDependentSource(float: Float): Dependent<Source> =
    if (float.isNaN()) {
      dependentMathSource("NAN")
    } else {
      when (float) {
        Float.POSITIVE_INFINITY -> dependentMathSource("INFINITY")
        Float.NEGATIVE_INFINITY -> dependentMathSource("-INFINITY")
        Float.MAX_VALUE -> dependentFloatSource("FLT_MAX")
        else -> dependentSource("${float}f")
      }
    }

  private fun literalDependentSource(double: Double): Dependent<Source> =
    if (double.isNaN()) {
      dependentMathSource("NAN")
    } else {
      when (double) {
        Double.POSITIVE_INFINITY -> dependentMathSource("INFINITY")
        Double.NEGATIVE_INFINITY -> dependentMathSource("-INFINITY")
        Double.MAX_VALUE -> dependentFloatSource("DBL_MAX")
        else -> dependentSource("$double")
      }
    }

  companion object {
    // Kotlin types that use the simple ObjC name for the type.
    val KT_NATIVE_SIMPLE_OBJC_NAME_TYPES =
      setOf(
        "kotlin.Boolean",
        "kotlin.Char",
        "kotlin.Byte",
        "kotlin.Short",
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Float",
        "kotlin.Double",
        "kotlin.Number",
      )

    // Java JRE classes where we generate J2ObjC compat headers.
    // TODO(b/448061854): At least enable the primitive types.
    val KT_NATIVE_JRE_EXCLUDE =
      setOf(
        "java.io.EOFException",
        "java.io.FileNotFoundException",
        "java.io.IOException",
        "java.lang.ArithmeticException",
        "java.lang.AutoCloseable",
        "java.lang.AssertionError",
        "java.lang.Class",
        "java.lang.ClassCastException",
        "java.lang.Enum",
        "java.lang.Error",
        "java.lang.Exception",
        "java.lang.IndexOutOfBoundsException",
        "java.lang.IllegalStateException",
        "java.lang.Iterable",
        "java.lang.JsException",
        "java.lang.Math",
        "java.lang.NullPointerException",
        "java.lang.NumberFormatException",
        "java.lang.OutOfMemoryError",
        "java.lang.RuntimeException",
        "java.lang.StringBuilder",
        "java.lang.StringIndexOutOfBoundsException",
        "java.lang.UnsupportedOperationException",
        "java.lang.invoke.",
        "java.lang.ref.WeakReference",
        "java.nio.",
        "java.util.Base64",
        "java.util.ConcurrentModificationException",
        "java.util.Iterator",
        "java.util.ListIterator",
        "java.util.Locale",
        "java.util.Map",
        "java.util.Map.Entry",
        "java.util.NoSuchElementException",
        "java.util.regex.",
        "java.util.concurrent.atomic.",
        "java.util.concurrent.KotlinExecutor",
        "javaemul.",
        "javax.",
      )

    // A map from the bridge name supplied by the KtNative annotation to the bridge name
    // that should be used for static methods / constructors.
    val KT_NATIVE_BRIDGE_NAME_MAP =
      mapOf(
        // The annotation value for Throwable doesn't match what we'd expect here as
        // java.lang.Throwable can't be used as the bridge type for Kotlin/JVM or J2cl.
        "javaemul.lang.ThrowableJvm" to "java.lang.Throwable"
      )
  }
}
