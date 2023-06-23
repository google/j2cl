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
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.FieldDescriptor
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
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.companionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.companionObjectOrNull
import com.google.j2cl.transpiler.backend.kotlin.ast.declaration
import com.google.j2cl.transpiler.backend.kotlin.common.buildList
import com.google.j2cl.transpiler.backend.kotlin.common.code
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.runIf
import com.google.j2cl.transpiler.backend.kotlin.common.titleCase
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependency
import com.google.j2cl.transpiler.backend.kotlin.objc.Import
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer
import com.google.j2cl.transpiler.backend.kotlin.objc.className
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.objc.compatibilityAlias
import com.google.j2cl.transpiler.backend.kotlin.objc.defineAlias
import com.google.j2cl.transpiler.backend.kotlin.objc.dependency
import com.google.j2cl.transpiler.backend.kotlin.objc.emptyRenderer
import com.google.j2cl.transpiler.backend.kotlin.objc.expressionStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.flatten
import com.google.j2cl.transpiler.backend.kotlin.objc.functionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.objc.getProperty
import com.google.j2cl.transpiler.backend.kotlin.objc.id
import com.google.j2cl.transpiler.backend.kotlin.objc.localImport
import com.google.j2cl.transpiler.backend.kotlin.objc.macroDefine
import com.google.j2cl.transpiler.backend.kotlin.objc.map
import com.google.j2cl.transpiler.backend.kotlin.objc.map2
import com.google.j2cl.transpiler.backend.kotlin.objc.methodCall
import com.google.j2cl.transpiler.backend.kotlin.objc.nsAssumeNonnull
import com.google.j2cl.transpiler.backend.kotlin.objc.nsCopying
import com.google.j2cl.transpiler.backend.kotlin.objc.nsEnumTypedef
import com.google.j2cl.transpiler.backend.kotlin.objc.nsInline
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableArray
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableDictionary
import com.google.j2cl.transpiler.backend.kotlin.objc.nsMutableSet
import com.google.j2cl.transpiler.backend.kotlin.objc.nsNumber
import com.google.j2cl.transpiler.backend.kotlin.objc.nsObject
import com.google.j2cl.transpiler.backend.kotlin.objc.nsString
import com.google.j2cl.transpiler.backend.kotlin.objc.pointer
import com.google.j2cl.transpiler.backend.kotlin.objc.protocolName
import com.google.j2cl.transpiler.backend.kotlin.objc.rendererOf
import com.google.j2cl.transpiler.backend.kotlin.objc.rendererWith
import com.google.j2cl.transpiler.backend.kotlin.objc.returnStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.semicolonEnded
import com.google.j2cl.transpiler.backend.kotlin.objc.sourceWithDependencies
import com.google.j2cl.transpiler.backend.kotlin.objc.toNullable
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.plusNewLine
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

internal val CompilationUnit.j2ObjCCompatHeaderSource: Source
  get() =
    dependenciesAndDeclarationsSource.ifNotEmpty {
      emptyLineSeparated(fileCommentSource, it).plusNewLine
    }

private val CompilationUnit.fileCommentSource: Source
  get() = comment(source("Generated by J2KT from \"${packageRelativePath}\""))

private val CompilationUnit.dependenciesAndDeclarationsSource: Source
  get() = declarationsRenderer.sourceWithDependencies

private val CompilationUnit.declarationsRenderer: Renderer<Source>
  get() = nsAssumeNonnull(declarationsRenderers.flatten.map(::emptyLineSeparated))

private val CompilationUnit.declarationsRenderers: List<Renderer<Source>>
  get() = includedTypes.flatMap(Type::declarationsRenderers)

private val CompilationUnit.includedTypes: List<Type>
  get() = types.filter { it.shouldRender }.flatMap { listOf(it) + it.includedTypes }

private val Type.includedTypes: List<Type>
  get() = types.filter { it.shouldRender }.flatMap { listOf(it) + it.includedTypes }

private val Type.shouldRender: Boolean
  get() = declaration.shouldRender

private val Type.declarationsRenderers: List<Renderer<Source>>
  get() =
    buildList<Renderer<Source>> {
      add(declaration.aliasDeclarationRenderer)

      companionObjectOrNull?.let { add(it.declaration.aliasDeclarationRenderer) }

      if (isEnum) {
        add(nsEnumTypedefRenderer)
      }

      addAll(fields.map { it.fieldConstantDefineRenderer })

      addAll(members.flatMap { it.functionRenderers })
    }

private val TypeDeclaration.aliasDeclarationRenderer: Renderer<Source>
  get() =
    objCNameRenderer.map { objCName ->
      when (kind!!) {
        TypeDeclaration.Kind.CLASS,
        TypeDeclaration.Kind.ENUM -> semicolonEnded(compatibilityAlias(source(objCAlias), objCName))
        TypeDeclaration.Kind.INTERFACE -> defineAlias(source(objCAlias), objCName)
      }
    }

private val CompanionDeclaration.aliasDeclarationRenderer: Renderer<Source>
  get() =
    objCNameRenderer.map { objCName ->
      semicolonEnded(compatibilityAlias(source(objCAlias), objCName))
    }

private val TypeDeclaration.objCAlias: String
  get() = objCNameWithoutPrefix

private val CompanionDeclaration.objCAlias: String
  get() = objCNameWithoutPrefix

private val Type.nsEnumTypedefRenderer: Renderer<Source>
  get() =
    nsEnumTypedef(
      name = declaration.objCEnumName,
      type = jintTypeRenderer,
      values = enumFields.map { it.descriptor.objCEnumName }
    )

private val FieldDescriptor.propertyQualifierRenderer: Renderer<Source>
  get() =
    enclosingTypeDescriptor.typeDeclaration.run {
      if (isStatic && !isEnumConstant) companionDeclaration.sharedRenderer else objCNameRenderer
    }

private val FieldDescriptor.getPropertyObjCName: String
  get() = if (isEnumConstant) objCName.escapeObjCEnumProperty else objCName.escapeObjCProperty

private val Field.fieldGetFunctionRenderer: Renderer<Source>
  get() = descriptor.takeIf { it.shouldRender }?.getFunctionRenderer ?: emptyRenderer

private val FieldDescriptor.getFunctionRenderer: Renderer<Source>
  get() =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = typeDescriptor.objCRenderer,
      name = getFunctionName,
      statements = listOf(returnStatement(getExpressionRenderer))
    )

private val FieldDescriptor.getFunctionName: String
  get() = enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix + "_get_" + name!!.objCName

private val FieldDescriptor.getExpressionRenderer: Renderer<Source>
  get() = propertyQualifierRenderer.map { dotSeparated(it, source(getPropertyObjCName)) }

private val FieldDescriptor.setPropertyObjCName: String
  get() = objCName.escapeObjCKeyword

private val Field.fieldSetFunctionRenderer: Renderer<Source>
  get() = descriptor.takeIf { !it.isFinal && it.shouldRender }?.setFunctionRenderer ?: emptyRenderer

private val FieldDescriptor.setFunctionRenderer: Renderer<Source>
  get() =
    functionDeclaration(
      modifiers = listOf(nsInline),
      returnType = PrimitiveTypes.VOID.objCRenderer,
      name = setFunctionName,
      parameters = listOf(setParameterRenderer),
      statements = listOf(setStatementRenderer)
    )

private val FieldDescriptor.setFunctionName: String
  get() = enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix + "_set_" + name!!.objCName

private val FieldDescriptor.setStatementRenderer: Renderer<Source>
  get() =
    propertyQualifierRenderer.map {
      semicolonEnded(
        assignment(dotSeparated(it, source(setPropertyObjCName)), source(setFunctionParameterName))
      )
    }

private val FieldDescriptor.setParameterRenderer: Renderer<Source>
  get() = typeDescriptor.objCRenderer.map { spaceSeparated(it, source(setFunctionParameterName)) }

private val setFunctionParameterName: String
  get() = "value"

private val Field.fieldConstantDefineRenderer: Renderer<Source>
  get() = descriptor.takeIf { it.shouldRender }?.constantDefineRenderer ?: emptyRenderer

private val FieldDescriptor.constantDefineRenderer: Renderer<Source>?
  get() =
    constantValue?.let(::constantValueRenderer)?.map { literalSource ->
      macroDefine(spaceSeparated(source(defineConstantName), literalSource))
    }

private val FieldDescriptor.defineConstantName: String
  get() = enclosingTypeDescriptor.typeDeclaration.objCNameWithoutPrefix + "_" + name!!

private val Member.functionRenderers: List<Renderer<Source>>
  get() =
    when (this) {
      is Method -> listOf(methodFunctionRenderer)
      is Field -> listOf(fieldGetFunctionRenderer, fieldSetFunctionRenderer)
      else -> listOf()
    }

private val Method.methodFunctionRenderer: Renderer<Source>
  get() =
    takeIf { it.descriptor.shouldRender }?.toObjCNames()?.let(::functionRenderer) ?: emptyRenderer

private val MethodDescriptor.shouldRender: Boolean
  get() =
    visibility.isPublic &&
      // Don't render constructors for inner-classes, because they have implicit `outer` parameter.
      (isStatic || (isConstructor && !enclosingTypeDescriptor.typeDeclaration.isKtInner)) &&
      returnTypeDescriptor.shouldRender &&
      parameterTypeDescriptors.all { it.shouldRender }

private val FieldDescriptor.shouldRender: Boolean
  get() =
    visibility.isPublic &&
      (isStatic || enclosingTypeDescriptor.isInterface) &&
      typeDescriptor.shouldRender

private val TypeDescriptor.shouldRender: Boolean
  get() =
    when (this) {
      is DeclaredTypeDescriptor -> typeDeclaration.shouldRender
      is ArrayTypeDescriptor -> false
      else -> true
    }

private val collectionTypeDescriptors: Set<TypeDescriptor>
  get() = setOf(typeDescriptors.javaUtilCollection, typeDescriptors.javaUtilMap)

private val TypeDeclaration.isCollection: Boolean
  get() =
    toUnparameterizedTypeDescriptor().run { collectionTypeDescriptors.any { isAssignableTo(it) } }

private val TypeDeclaration.shouldRender: Boolean
  get() = visibility.isPublic && existsInObjC && !isCollection

private val TypeDeclaration.existsInObjC: Boolean
  get() = !isKtNative || mappedObjCNameRenderer != null

private fun Method.functionRenderer(objCNames: MethodObjCNames): Renderer<Source> =
  functionDeclaration(
    modifiers = listOf(nsInline),
    returnType = descriptor.returnTypeDescriptor.objCRenderer,
    name = descriptor.functionName(objCNames),
    parameters = parameters.map { it.renderer },
    statements = statementRenderers(objCNames.escapeObjCMethod(isConstructor))
  )

private fun MethodDescriptor.functionName(objCNames: MethodObjCNames): String =
  enclosingTypeDescriptor
    .objCName(useId = true)
    .letIf(isConstructor) { "create_$it" }
    .plus("_")
    .plus(objCNames.methodName)
    .letIf(objCNames.parameterNames.isNotEmpty()) { parameterName ->
      parameterName.plus(
        objCNames.parameterNames
          .mapIndexed { index, name ->
            name.letIf(index == 0) { it.titleCase.letIf(isConstructor) { "With$it" } }.plus("_")
          }
          .joinToString("")
      )
    }

private fun Method.statementRenderers(objCNames: MethodObjCNames): List<Renderer<Source>> =
  if (isPrimitiveVoid(descriptor.returnTypeDescriptor))
    listOf(expressionStatement(methodCallRenderer(objCNames)))
  else listOf(returnStatement(methodCallRenderer(objCNames)))

private fun Method.methodCallRenderer(objCNames: MethodObjCNames): Renderer<Source> =
  methodCall(
    target = methodCallTargetRenderer,
    name = objCNames.objCSelector,
    arguments = parameters.map { it.nameRenderer }
  )

private val Method.methodCallTargetRenderer: Renderer<Source>
  get() =
    if (isConstructor) descriptor.enclosingTypeDescriptor.typeDeclaration.allocRenderer
    else descriptor.enclosingTypeDescriptor.typeDeclaration.companionDeclaration.sharedRenderer

private val MethodObjCNames.objCSelector: String
  get() =
    methodName.plus(
      parameterNames
        .mapIndexed { index, name -> name.letIf(index == 0) { it.titleCase } + ":" }
        .joinToString("")
    )

private val Variable.renderer: Renderer<Source>
  get() =
    map2(typeDescriptor.objCRenderer, nameRenderer) { typeSource, nameSource ->
      spaceSeparated(typeSource, nameSource)
    }

private val Variable.nameRenderer: Renderer<Source>
  get() = rendererOf(source(name.objCName.escapeObjCKeyword))

private val CompanionDeclaration.objCNameRenderer: Renderer<Source>
  get() = className(objCName)

private val CompanionDeclaration.sharedRenderer: Renderer<Source>
  get() = getProperty(objCNameRenderer, "shared")

private val TypeDeclaration.allocRenderer: Renderer<Source>
  get() = objCNameRenderer.map { inSquareBrackets(spaceSeparated(it, source("alloc"))) }

private val TypeDeclaration.objCNameRenderer: Renderer<Source>
  get() = mappedObjCNameRenderer ?: nonMappedObjCNameRenderer

private val TypeDeclaration.mappedObjCNameRenderer: Renderer<Source>?
  get() =
    when (qualifiedBinaryName) {
      "java.lang.Object" -> nsObject
      "java.lang.String" -> nsString
      "java.lang.Number" -> nsNumber
      "java.lang.Cloneable" -> nsCopying
      "java.util.List" -> nsMutableArray
      "java.util.Set" -> nsMutableSet
      "java.util.Map" -> nsMutableDictionary
      else -> null
    }

private val TypeDeclaration.nonMappedObjCNameRenderer: Renderer<Source>
  get() = kind.objCNameRenderer(objCName)

private fun TypeDeclaration.Kind.objCNameRenderer(name: String): Renderer<Source> =
  when (this) {
    TypeDeclaration.Kind.CLASS,
    TypeDeclaration.Kind.ENUM -> className(name)
    TypeDeclaration.Kind.INTERFACE -> protocolName(name)
  }

private val TypeDescriptor.objCRenderer: Renderer<Source>
  get() =
    when (this) {
      is PrimitiveTypeDescriptor -> primitiveObjCRenderer
      is DeclaredTypeDescriptor -> declaredObjCRenderer
      // TODO: Handle TypeVariable and Array
      else -> id
    }.runIf(isNullable) { toNullable() }

private val PrimitiveTypeDescriptor.primitiveObjCRenderer: Renderer<Source>
  get() =
    when (this) {
      PrimitiveTypes.VOID -> rendererOf(source("void"))
      PrimitiveTypes.BOOLEAN -> jbooleanTypeRenderer
      PrimitiveTypes.CHAR -> jcharTypeRenderer
      PrimitiveTypes.BYTE -> jbyteTypeRenderer
      PrimitiveTypes.SHORT -> jshortTypeRenderer
      PrimitiveTypes.INT -> jintTypeRenderer
      PrimitiveTypes.LONG -> jlongTypeRenderer
      PrimitiveTypes.FLOAT -> jfloatTypeRenderer
      PrimitiveTypes.DOUBLE -> jdoubleTypeRenderer
      else -> error("$this.primitiveObjCRenderer")
    }

private val DeclaredTypeDescriptor.declaredObjCRenderer: Renderer<Source>
  get() =
    when {
      isJavaLangObject(this) -> id
      isInterface -> interfaceObjCRenderer
      else -> typeDeclaration.objCNameRenderer.map(::pointer)
    }

private val DeclaredTypeDescriptor.interfaceObjCRenderer: Renderer<Source>
  get() =
    map2(id, typeDeclaration.objCNameRenderer) { idSource, typeSource ->
      join(idSource, inAngleBrackets(typeSource))
    }

private val j2ObjCTypesImport: Import
  get() = localImport("third_party/java_src/j2objc/jre_emul/Classes/J2ObjC_types.h")

private val j2ObjCTypesDependency: Dependency
  get() = dependency(j2ObjCTypesImport)

private fun j2ObjCTypeRenderer(name: String): Renderer<Source> =
  source(name) rendererWith j2ObjCTypesDependency

private val jbooleanTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jboolean")

private val jcharTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jchar")

private val jbyteTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jbyte")

private val jshortTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jshort")

private val jintTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jint")

private val jlongTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jlong")

private val jfloatTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jfloat")

private val jdoubleTypeRenderer: Renderer<Source>
  get() = j2ObjCTypeRenderer("jdouble")

private val TypeDeclaration.objCEnumName: String
  get() = "${objCNameWithoutPrefix}_Enum"

private val FieldDescriptor.objCEnumName: String
  get() = "${enclosingTypeDescriptor.typeDeclaration.objCEnumName}_$name"

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
  rendererOf(source(if (boolean) "true" else "false"))

private fun literalRenderer(char: Char): Renderer<Source> =
  char.code.let { code ->
    if (code in 0x20..0x7E)
      when (char) {
        '\'' -> rendererOf(source("'\\''"))
        '\\' -> rendererOf(source("'\\\\'"))
        else -> rendererOf(source("'$char'"))
      }
    else rendererOf(source(String.format("0x%04x", code)))
  }

private fun literalRenderer(byte: Byte): Renderer<Source> = rendererOf(source("$byte"))

private fun literalRenderer(short: Short): Renderer<Source> = rendererOf(source("$short"))

private fun literalRenderer(int: Int): Renderer<Source> =
  when (int) {
    Int.MIN_VALUE ->
      jintTypeRenderer.map {
        inRoundBrackets(spaceSeparated(inRoundBrackets(it), source("0x80000000")))
      }
    else -> rendererOf(source("$int"))
  }

private fun literalRenderer(long: Long): Renderer<Source> =
  when (long) {
    Long.MIN_VALUE ->
      jlongTypeRenderer.map {
        inRoundBrackets(spaceSeparated(inRoundBrackets(it), source("0x8000000000000000LL")))
      }
    else -> rendererOf(source("${long}LL"))
  }

private fun literalRenderer(float: Float): Renderer<Source> =
  if (float.isNaN()) rendererOf(source("NAN"))
  else
    when (float) {
      Float.POSITIVE_INFINITY -> rendererOf(source("INFINITY"))
      Float.NEGATIVE_INFINITY -> rendererOf(source("-INFINITY"))
      Float.MAX_VALUE -> rendererOf(source("__FLT_MAX__"))
      else -> rendererOf(source("${float}f"))
    }

private fun literalRenderer(double: Double): Renderer<Source> =
  if (double.isNaN()) rendererOf(source("NAN"))
  else
    when (double) {
      Double.POSITIVE_INFINITY -> rendererOf(source("INFINITY"))
      Double.NEGATIVE_INFINITY -> rendererOf(source("-INFINITY"))
      Double.MAX_VALUE -> rendererOf(source("__DLB_MAX__"))
      else -> rendererOf(source("$double"))
    }
