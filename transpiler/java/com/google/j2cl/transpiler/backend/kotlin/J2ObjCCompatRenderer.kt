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
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.flatten
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.rendererOf
import com.google.j2cl.transpiler.backend.kotlin.objc.className
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.objc.defineAlias
import com.google.j2cl.transpiler.backend.kotlin.objc.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.objc.expressionStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.floatSourceRenderer
import com.google.j2cl.transpiler.backend.kotlin.objc.functionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.objc.getProperty
import com.google.j2cl.transpiler.backend.kotlin.objc.id
import com.google.j2cl.transpiler.backend.kotlin.objc.macroDefine
import com.google.j2cl.transpiler.backend.kotlin.objc.mathSourceRenderer
import com.google.j2cl.transpiler.backend.kotlin.objc.methodCall
import com.google.j2cl.transpiler.backend.kotlin.objc.nsAssumeNonnull
import com.google.j2cl.transpiler.backend.kotlin.objc.nsCopying
import com.google.j2cl.transpiler.backend.kotlin.objc.nsEnumTypedef
import com.google.j2cl.transpiler.backend.kotlin.objc.nsInline
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableArray
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableDictionary
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableSet
import com.google.j2cl.transpiler.backend.kotlin.objc.nsNumber
import com.google.j2cl.transpiler.backend.kotlin.objc.nsObjCRuntimeSourceRenderer
import com.google.j2cl.transpiler.backend.kotlin.objc.nsObject
import com.google.j2cl.transpiler.backend.kotlin.objc.nsString
import com.google.j2cl.transpiler.backend.kotlin.objc.plusAssignment
import com.google.j2cl.transpiler.backend.kotlin.objc.plusSemicolon
import com.google.j2cl.transpiler.backend.kotlin.objc.protocolName
import com.google.j2cl.transpiler.backend.kotlin.objc.returnStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.sourceRenderer
import com.google.j2cl.transpiler.backend.kotlin.objc.sourceWithDependencies
import com.google.j2cl.transpiler.backend.kotlin.objc.toNullable
import com.google.j2cl.transpiler.backend.kotlin.objc.toPointer
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
    declarationsRenderer(compilationUnit).sourceWithDependencies

  private fun declarationsRenderer(compilationUnit: CompilationUnit): Renderer<Source> =
    nsAssumeNonnull(declarationsRenderers(compilationUnit).flatten().map { emptyLineSeparated(it) })

  private fun declarationsRenderers(compilationUnit: CompilationUnit): List<Renderer<Source>> =
    includedTypes(compilationUnit).flatMap(::declarationsRenderers)

  private fun includedTypes(compilationUnit: CompilationUnit): List<Type> =
    compilationUnit.types.filter(::shouldRender).flatMap { listOf(it) + includedTypes(it) }

  private fun includedTypes(type: Type): List<Type> =
    type.types.filter(::shouldRender).flatMap { listOf(it) + includedTypes(it) }

  private fun shouldRender(type: Type): Boolean =
    !nameIsMappedInObjC(type.declaration) && shouldRender(type.declaration)

  private fun declarationsRenderers(type: Type): List<Renderer<Source>> = buildList {
    if (objCNamePrefix.isNotEmpty()) {
      add(aliasDeclarationRenderer(type.declaration))
      type.toCompanionObjectOrNull()?.let { add(aliasDeclarationRenderer(it.declaration)) }
    }

    if (type.isEnum) {
      add(nsEnumTypedefRenderer(type))
    }

    addAll(type.fields.map(::fieldConstantDefineRenderer))

    addAll(type.members.flatMap(::functionRenderers))

    // Render implicit constructor
    if (!type.isInterface && type.constructors.isEmpty()) {
      addAll(
        functionRenderers(
          Method.newBuilder()
            .setMethodDescriptor(createImplicitConstructorDescriptor(type.typeDescriptor))
            .setSourcePosition(type.sourcePosition)
            .build()
        )
      )
    }
  }

  private fun aliasDeclarationRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    objCNameRenderer(typeDeclaration).map { objCName ->
      defineAlias(source(objCAlias(typeDeclaration)), objCName)
    }

  private fun aliasDeclarationRenderer(
    companionDeclaration: CompanionDeclaration
  ): Renderer<Source> =
    objCNameRenderer(companionDeclaration).map { objCName ->
      defineAlias(source(objCAlias(companionDeclaration)), objCName)
    }

  private fun objCAlias(typeDeclaration: TypeDeclaration): String =
    typeDeclaration.objCNameWithoutPrefix

  private fun objCAlias(companionDeclaration: CompanionDeclaration): String =
    companionDeclaration.objCNameWithoutPrefix

  private fun nsEnumTypedefRenderer(type: Type): Renderer<Source> =
    nsEnumTypedef(
      name = objCEnumName(type.declaration),
      type = intTypeRenderer,
      values = type.enumFields.map { objCEnumName(it.descriptor) },
    )

  private fun propertyQualifierRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source> =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.let { typeDeclaration ->
      if (fieldDescriptor.isStatic && !fieldDescriptor.isEnumConstant) {
        sharedRenderer(typeDeclaration.companionDeclaration)
      } else {
        objCNameRenderer(typeDeclaration)
      }
    }

  private fun getPropertyObjCName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.objCName

  private fun fieldGetFunctionRenderer(field: Field): Renderer<Source> =
    field.descriptor.takeIf(::shouldRender)?.let(::getFunctionRenderer) ?: rendererOf(Source.EMPTY)

  private fun getFunctionRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source> =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = objCRenderer(fieldDescriptor.typeDescriptor),
      name = getFunctionName(fieldDescriptor),
      statements = listOf(returnStatement(getExpressionRenderer(fieldDescriptor))),
    )

  private fun getFunctionName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix +
      "_get_" +
      fieldDescriptor.name!!.objCName

  private fun getExpressionRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source> =
    if (fieldDescriptor.typeDescriptor.isPrimitive && fieldDescriptor.isCompileTimeConstant) {
      sourceRenderer(defineConstantName(fieldDescriptor))
    } else {
      propertyQualifierRenderer(fieldDescriptor).map {
        dotSeparated(it, source(getPropertyObjCName(fieldDescriptor)))
      }
    }

  private fun setPropertyObjCName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.objCName.escapeObjCKeyword

  private fun fieldSetFunctionRenderer(field: Field): Renderer<Source> =
    field.descriptor.takeIf { !it.isFinal && shouldRender(it) }?.let(::setFunctionRenderer)
      ?: rendererOf(Source.EMPTY)

  private fun setFunctionRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source> =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = objCRenderer(PrimitiveTypes.VOID),
      name = setFunctionName(fieldDescriptor),
      parameters = listOf(setParameterRenderer(fieldDescriptor)),
      statements = listOf(setStatementRenderer(fieldDescriptor)),
    )

  private fun setFunctionName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix +
      "_set_" +
      fieldDescriptor.name!!.objCName

  private fun setStatementRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source> =
    dotSeparated(
        propertyQualifierRenderer(fieldDescriptor),
        sourceRenderer(setPropertyObjCName(fieldDescriptor)),
      )
      .plusAssignment(sourceRenderer(setFunctionParameterName))
      .plusSemicolon()

  private fun setParameterRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source> =
    objCRenderer(fieldDescriptor.typeDescriptor).map {
      spaceSeparated(it, source(setFunctionParameterName))
    }

  private val setFunctionParameterName: String
    get() = "value"

  private fun fieldConstantDefineRenderer(field: Field): Renderer<Source> =
    field.descriptor.takeIf(::shouldRender)?.let(::constantDefineRenderer)
      ?: rendererOf(Source.EMPTY)

  private fun constantDefineRenderer(fieldDescriptor: FieldDescriptor): Renderer<Source>? =
    fieldDescriptor.constantValue?.let(::constantValueRenderer)?.map { literalSource ->
      macroDefine(spaceSeparated(source(defineConstantName(fieldDescriptor)), literalSource))
    }

  private fun defineConstantName(fieldDescriptor: FieldDescriptor): String =
    fieldDescriptor.enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix +
      "_" +
      fieldDescriptor.name!!

  private fun functionRenderers(member: Member): List<Renderer<Source>> =
    when (member) {
      is Method -> methodFunctionRenderers(member)
      is Field -> listOf(fieldGetFunctionRenderer(member), fieldSetFunctionRenderer(member))
      else -> emptyList()
    }

  private fun methodFunctionRenderers(method: Method): List<Renderer<Source>> =
    method
      .takeIf { shouldRender(it.descriptor) }
      ?.toObjCNames()
      ?.let { functionRenderers(method, it) } ?: listOf()

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
      mappedObjCNameRenderer(typeDeclaration) != null

  private fun functionRenderers(
    method: Method,
    objCNames: MethodObjCNames,
  ): List<Renderer<Source>> =
    if (method.isConstructor) {
      listOf(
        functionRenderer(method, objCNames, prefix = "create_"),
        functionRenderer(method, objCNames, prefix = "new_"),
      )
    } else {
      listOf(functionRenderer(method, objCNames))
    }

  private fun functionRenderer(
    method: Method,
    objCNames: MethodObjCNames,
    prefix: String = "",
  ): Renderer<Source> =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = objCRenderer(method.descriptor.returnTypeDescriptor),
      name = functionName(method.descriptor, objCNames, prefix),
      parameters = method.parameters.map(::renderer),
      statements = statementRenderers(method, objCNames.escapeObjCMethod(method.isConstructor)),
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

  private fun statementRenderers(
    method: Method,
    objCNames: MethodObjCNames,
  ): List<Renderer<Source>> =
    if (isPrimitiveVoid(method.descriptor.returnTypeDescriptor)) {

      listOf(expressionStatement(methodCallRenderer(method, objCNames)))
    } else {
      listOf(returnStatement(methodCallRenderer(method, objCNames)))
    }

  private fun methodCallRenderer(method: Method, objCNames: MethodObjCNames): Renderer<Source> =
    methodCall(
      target = methodCallTargetRenderer(method),
      name = objCSelector(objCNames),
      arguments = method.parameters.map(::nameRenderer),
    )

  private fun methodCallTargetRenderer(method: Method): Renderer<Source> =
    if (method.isConstructor) {
      allocRenderer(method.descriptor.enclosingTypeDescriptor.typeDeclaration)
    } else {
      sharedRenderer(method.descriptor.enclosingTypeDescriptor.typeDeclaration.companionDeclaration)
    }

  private fun objCSelector(methodObjCNames: MethodObjCNames): String =
    methodObjCNames.objCName.string.plus(
      methodObjCNames.parameterObjCNames
        .mapIndexed { index, name -> name.string.letIf(index == 0) { it.titleCased } + ":" }
        .joinToString("")
    )

  private fun renderer(variable: Variable): Renderer<Source> =
    combine(objCRenderer(variable.typeDescriptor), nameRenderer(variable)) { typeSource, nameSource
      ->
      spaceSeparated(typeSource, nameSource)
    }

  private fun nameRenderer(variable: Variable): Renderer<Source> =
    sourceRenderer(variable.name.objCName.escapeObjCKeyword)

  private fun objCNameRenderer(companionDeclaration: CompanionDeclaration): Renderer<Source> =
    mappedKtNativeCompanionRenderer(companionDeclaration.enclosingTypeDeclaration)
      ?: className(companionDeclaration.objCName(objCNamePrefix))

  private fun sharedRenderer(companionDeclaration: CompanionDeclaration): Renderer<Source> =
    getProperty(objCNameRenderer(companionDeclaration), "shared")

  private fun allocRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    (mappedKtNativeBridgeRenderer(typeDeclaration) ?: objCNameRenderer(typeDeclaration)).map {
      inSquareBrackets(spaceSeparated(it, source("alloc")))
    }

  private fun objCNameRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    mappedObjCNameRenderer(typeDeclaration)
      ?: mappedKtNativeRenderer(typeDeclaration)
      ?: nonMappedObjCNameRenderer(typeDeclaration)

  private fun nameIsMappedInObjC(typeDeclaration: TypeDeclaration): Boolean =
    mappedObjCNameRenderer(typeDeclaration) != null

  private fun mappedObjCNameRenderer(typeDeclaration: TypeDeclaration): Renderer<Source>? =
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

  private fun mappedKtNativeRenderer(kind: TypeDeclaration.Kind, name: String): Renderer<Source>? =
    objCNameRenderer(kind, ktNativeNameToObjCName(name))

  private fun mappedKtNativeRenderer(typeDeclaration: TypeDeclaration): Renderer<Source>? =
    typeDeclaration.ktNativeQualifiedName?.let { mappedKtNativeRenderer(typeDeclaration.kind, it) }

  private fun mappedKtNativeBridgeRenderer(typeDeclaration: TypeDeclaration): Renderer<Source>? =
    KT_NATIVE_BRIDGE_NAME_MAP.get(
        typeDeclaration.ktBridgeQualifiedName ?: typeDeclaration.ktNativeQualifiedName
      )
      .orIfNull { typeDeclaration.ktBridgeQualifiedName }
      .orIfNull { typeDeclaration.ktNativeQualifiedName }
      ?.let { mappedKtNativeRenderer(typeDeclaration.kind, it) }

  private fun mappedKtNativeCompanionRenderer(typeDeclaration: TypeDeclaration): Renderer<Source>? =
    typeDeclaration.ktCompanionQualifiedName
      .orIfNull { typeDeclaration.ktNativeQualifiedName }
      ?.let { mappedKtNativeRenderer(typeDeclaration.kind, it + "Companion") }

  private fun nonMappedObjCNameRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    objCNameRenderer(typeDeclaration.kind, typeDeclaration.objCName(prefix = objCNamePrefix))

  private fun objCNameRenderer(kind: TypeDeclaration.Kind, name: String): Renderer<Source> =
    when (kind) {
      TypeDeclaration.Kind.CLASS,
      TypeDeclaration.Kind.ENUM -> className(name)
      TypeDeclaration.Kind.INTERFACE -> protocolName(name)
    }

  private fun objCRenderer(typeDescriptor: TypeDescriptor): Renderer<Source> =
    when {
      typeDescriptor is PrimitiveTypeDescriptor -> primitiveObjCRenderer(typeDescriptor)
      typeDescriptor is DeclaredTypeDescriptor && !typeDescriptor.typeDeclaration.isProtobuf ->
        declaredObjCRenderer(typeDescriptor)
      // TODO: Handle TypeVariable and Array
      else -> id
    }.runIf(typeDescriptor.isNullable) { toNullable() }

  private fun primitiveObjCRenderer(
    primitiveTypeDescriptor: PrimitiveTypeDescriptor
  ): Renderer<Source> =
    when (primitiveTypeDescriptor) {
      PrimitiveTypes.VOID -> sourceRenderer("void")
      PrimitiveTypes.BOOLEAN -> booleanTypeRenderer
      PrimitiveTypes.CHAR -> charTypeRenderer
      PrimitiveTypes.BYTE -> byteTypeRenderer
      PrimitiveTypes.SHORT -> shortTypeRenderer
      PrimitiveTypes.INT -> intTypeRenderer
      PrimitiveTypes.LONG -> longTypeRenderer
      PrimitiveTypes.FLOAT -> floatTypeRenderer
      PrimitiveTypes.DOUBLE -> doubleTypeRenderer
      else -> error("$this.primitiveObjCRenderer")
    }

  private fun declaredObjCRenderer(
    declaredTypeDescriptor: DeclaredTypeDescriptor
  ): Renderer<Source> =
    when {
      isJavaLangObject(declaredTypeDescriptor) -> id
      declaredTypeDescriptor.isInterface -> interfaceObjCRenderer(declaredTypeDescriptor)
      else -> objCNameRenderer(declaredTypeDescriptor.typeDeclaration).toPointer()
    }

  private fun interfaceObjCRenderer(
    declaredTypeDescriptor: DeclaredTypeDescriptor
  ): Renderer<Source> =
    combine(id, objCNameRenderer(declaredTypeDescriptor.typeDeclaration)) { idSource, typeSource ->
      join(idSource, inAngleBrackets(typeSource))
    }

  private val booleanTypeRenderer: Renderer<Source>
    get() = nsObjCRuntimeSourceRenderer("BOOL")

  private val charTypeRenderer: Renderer<Source>
    get() = nsObjCRuntimeSourceRenderer("uint16_t")

  private val byteTypeRenderer: Renderer<Source>
    get() = nsObjCRuntimeSourceRenderer("int8_t")

  private val shortTypeRenderer: Renderer<Source>
    get() = nsObjCRuntimeSourceRenderer("int16_t")

  private val intTypeRenderer: Renderer<Source>
    get() = nsObjCRuntimeSourceRenderer("int32_t")

  private val longTypeRenderer: Renderer<Source>
    get() = nsObjCRuntimeSourceRenderer("int64_t")

  private val floatTypeRenderer: Renderer<Source>
    get() = sourceRenderer("float")

  private val doubleTypeRenderer: Renderer<Source>
    get() = sourceRenderer("double")

  private fun objCEnumName(typeDeclaration: TypeDeclaration): String =
    "${typeDeclaration.objCNameWithoutPrefix}_Enum"

  private fun objCEnumName(fieldDescriptor: FieldDescriptor): String =
    "${objCEnumName(fieldDescriptor.enclosingTypeDescriptor.typeDeclaration)}_${fieldDescriptor.name}"

  private fun constantValueRenderer(literal: Literal): Renderer<Source>? =
    when (literal) {
      is BooleanLiteral -> booleanLiteralRenderer(literal)
      is NumberLiteral -> numberLiteralRenderer(literal)
      else -> null
    }

  // Literal rendering code is based on:
  // /google3/third_party/java_src/j2objc/translator/src/main/java/com/google/devtools/j2objc/gen/LiteralGenerator.java

  private fun booleanLiteralRenderer(booleanLiteral: BooleanLiteral): Renderer<Source>? =
    literalRenderer(booleanLiteral.value)

  private fun numberLiteralRenderer(numberLiteral: NumberLiteral): Renderer<Source>? =
    when (numberLiteral.typeDescriptor) {
      PrimitiveTypes.CHAR -> literalRenderer(numberLiteral.value.toChar())
      PrimitiveTypes.BYTE -> literalRenderer(numberLiteral.value.toByte())
      PrimitiveTypes.SHORT -> literalRenderer(numberLiteral.value.toShort())
      PrimitiveTypes.INT -> literalRenderer(numberLiteral.value.toInt())
      PrimitiveTypes.LONG -> literalRenderer(numberLiteral.value.toLong())
      PrimitiveTypes.FLOAT -> literalRenderer(numberLiteral.value.toFloat())
      PrimitiveTypes.DOUBLE -> literalRenderer(numberLiteral.value.toDouble())
      else -> null
    }

  private fun literalRenderer(boolean: Boolean): Renderer<Source> =
    nsObjCRuntimeSourceRenderer(if (boolean) "true" else "false")

  private fun literalRenderer(char: Char): Renderer<Source> =
    sourceRenderer(
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

  private fun literalRenderer(byte: Byte): Renderer<Source> = sourceRenderer("$byte")

  private fun literalRenderer(short: Short): Renderer<Source> = sourceRenderer("$short")

  private fun literalRenderer(int: Int): Renderer<Source> =
    when (int) {
      Int.MIN_VALUE ->
        intTypeRenderer.map {
          inParentheses(spaceSeparated(inParentheses(it), source("0x80000000")))
        }
      else -> sourceRenderer("$int")
    }

  private fun literalRenderer(long: Long): Renderer<Source> =
    when (long) {
      Long.MIN_VALUE ->
        longTypeRenderer.map {
          inParentheses(spaceSeparated(inParentheses(it), source("0x8000000000000000LL")))
        }
      else -> sourceRenderer("${long}LL")
    }

  private fun literalRenderer(float: Float): Renderer<Source> =
    if (float.isNaN()) mathSourceRenderer("NAN")
    else
      when (float) {
        Float.POSITIVE_INFINITY -> mathSourceRenderer("INFINITY")
        Float.NEGATIVE_INFINITY -> mathSourceRenderer("-INFINITY")
        Float.MAX_VALUE -> floatSourceRenderer("FLT_MAX")
        else -> sourceRenderer("${float}f")
      }

  private fun literalRenderer(double: Double): Renderer<Source> =
    if (double.isNaN()) mathSourceRenderer("NAN")
    else
      when (double) {
        Double.POSITIVE_INFINITY -> mathSourceRenderer("INFINITY")
        Double.NEGATIVE_INFINITY -> mathSourceRenderer("-INFINITY")
        Double.MAX_VALUE -> floatSourceRenderer("DBL_MAX")
        else -> sourceRenderer("$double")
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
