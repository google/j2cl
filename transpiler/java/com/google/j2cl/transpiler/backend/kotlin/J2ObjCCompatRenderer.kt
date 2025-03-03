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
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.companionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.declaration
import com.google.j2cl.transpiler.backend.kotlin.ast.toCompanionObjectOrNull
import com.google.j2cl.transpiler.backend.kotlin.common.backslashEscapedString
import com.google.j2cl.transpiler.backend.kotlin.common.inSingleQuotes
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.runIf
import com.google.j2cl.transpiler.backend.kotlin.common.titleCased
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependency
import com.google.j2cl.transpiler.backend.kotlin.objc.Import
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.flatten
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.rendererOf
import com.google.j2cl.transpiler.backend.kotlin.objc.className
import com.google.j2cl.transpiler.backend.kotlin.objc.comment
import com.google.j2cl.transpiler.backend.kotlin.objc.compatibilityAlias
import com.google.j2cl.transpiler.backend.kotlin.objc.defineAlias
import com.google.j2cl.transpiler.backend.kotlin.objc.expressionStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.functionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.objc.getProperty
import com.google.j2cl.transpiler.backend.kotlin.objc.id
import com.google.j2cl.transpiler.backend.kotlin.objc.macroDefine
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
import com.google.j2cl.transpiler.backend.kotlin.objc.returnStatement
import com.google.j2cl.transpiler.backend.kotlin.objc.semicolonEnded
import com.google.j2cl.transpiler.backend.kotlin.objc.sourceWithDependencies
import com.google.j2cl.transpiler.backend.kotlin.objc.toNullable
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated

internal class J2ObjCCompatRenderer(private val withJ2ktPrefix: Boolean) {
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

  private fun shouldRender(type: Type): Boolean = shouldRender(type.declaration)

  private fun declarationsRenderers(type: Type): List<Renderer<Source>> = buildList {
    add(aliasDeclarationRenderer(type.declaration))

    type.toCompanionObjectOrNull()?.let { add(aliasDeclarationRenderer(it.declaration)) }

    if (type.isEnum) {
      add(nsEnumTypedefRenderer(type))
    }

    addAll(type.fields.map(::fieldConstantDefineRenderer))

    addAll(type.members.flatMap(::functionRenderers))
  }

  private fun aliasDeclarationRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    objCNameRenderer(typeDeclaration).map { objCName ->
      when (typeDeclaration.kind!!) {
        TypeDeclaration.Kind.CLASS,
        TypeDeclaration.Kind.ENUM ->
          semicolonEnded(compatibilityAlias(source(objCAlias(typeDeclaration)), objCName))
        TypeDeclaration.Kind.INTERFACE -> defineAlias(source(objCAlias(typeDeclaration)), objCName)
      }
    }

  private fun aliasDeclarationRenderer(
    companionDeclaration: CompanionDeclaration
  ): Renderer<Source> =
    objCNameRenderer(companionDeclaration).map { objCName ->
      semicolonEnded(compatibilityAlias(source(objCAlias(companionDeclaration)), objCName))
    }

  private fun objCAlias(typeDeclaration: TypeDeclaration): String =
    typeDeclaration.objCNameWithoutPrefix

  private fun objCAlias(companionDeclaration: CompanionDeclaration): String =
    companionDeclaration.objCNameWithoutPrefix

  private fun nsEnumTypedefRenderer(type: Type): Renderer<Source> =
    nsEnumTypedef(
      name = objCEnumName(type.declaration),
      type = jintTypeRenderer,
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
    if (fieldDescriptor.isEnumConstant) {
      fieldDescriptor.objCName.escapeObjCEnumProperty
    } else {
      fieldDescriptor.objCName.escapeObjCProperty
    }

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
    propertyQualifierRenderer(fieldDescriptor).map {
      dotSeparated(it, source(getPropertyObjCName(fieldDescriptor)))
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
    propertyQualifierRenderer(fieldDescriptor).map {
      semicolonEnded(
        assignment(
          dotSeparated(it, source(setPropertyObjCName(fieldDescriptor))),
          source(setFunctionParameterName),
        )
      )
    }

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
    methodDescriptor.visibility.isPublic &&
      // Don't render constructors for inner-classes, because they have implicit `outer`
      // parameter.
      (methodDescriptor.isStatic ||
        (methodDescriptor.isConstructor &&
          !methodDescriptor.enclosingTypeDescriptor.typeDeclaration.isKtInner)) &&
      shouldRender(methodDescriptor.returnTypeDescriptor) &&
      methodDescriptor.parameterTypeDescriptors.all(::shouldRender)

  private fun shouldRender(fieldDescriptor: FieldDescriptor): Boolean =
    fieldDescriptor.visibility.isPublic &&
      (fieldDescriptor.isStatic || fieldDescriptor.enclosingTypeDescriptor.isInterface) &&
      shouldRender(fieldDescriptor.typeDescriptor)

  private fun shouldRender(typeDescriptor: TypeDescriptor): Boolean =
    when (typeDescriptor) {
      is DeclaredTypeDescriptor -> shouldRender(typeDescriptor.typeDeclaration)
      is ArrayTypeDescriptor -> false
      else -> true
    }

  private val collectionTypeDescriptors: Set<TypeDescriptor>
    get() = setOf(typeDescriptors.javaUtilCollection, typeDescriptors.javaUtilMap)

  private fun isCollection(typeDeclaration: TypeDeclaration): Boolean =
    typeDeclaration.toDescriptor().run { collectionTypeDescriptors.any { isAssignableTo(it) } }

  private fun shouldRender(typeDeclaration: TypeDeclaration): Boolean =
    typeDeclaration.visibility.isPublic &&
      existsInObjC(typeDeclaration) &&
      !isCollection(typeDeclaration) &&
      !typeDeclaration.isProtobuf

  private fun existsInObjC(typeDeclaration: TypeDeclaration): Boolean =
    !typeDeclaration.isKtNative || mappedObjCNameRenderer(typeDeclaration) != null

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
      .plus(objCNames.methodName)
      .letIf(objCNames.parameterNames.isNotEmpty()) { parameterName ->
        parameterName.plus(
          objCNames.parameterNames
            .mapIndexed { index, name ->
              name
                .letIf(index == 0) {
                  it.titleCased.letIf(methodDescriptor.isConstructor) { "With$it" }
                }
                .plus("_")
            }
            .joinToString("")
        )
      }

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
    methodObjCNames.methodName.plus(
      methodObjCNames.parameterNames
        .mapIndexed { index, name -> name.letIf(index == 0) { it.titleCased } + ":" }
        .joinToString("")
    )

  private fun renderer(variable: Variable): Renderer<Source> =
    combine(objCRenderer(variable.typeDescriptor), nameRenderer(variable)) { typeSource, nameSource
      ->
      spaceSeparated(typeSource, nameSource)
    }

  private fun nameRenderer(variable: Variable): Renderer<Source> =
    rendererOf(source(variable.name.objCName.escapeObjCKeyword))

  private fun objCNameRenderer(companionDeclaration: CompanionDeclaration): Renderer<Source> =
    className(companionDeclaration.objCName(withJ2ktPrefix))

  private fun sharedRenderer(companionDeclaration: CompanionDeclaration): Renderer<Source> =
    getProperty(objCNameRenderer(companionDeclaration), "shared")

  private fun allocRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    objCNameRenderer(typeDeclaration).map { inSquareBrackets(spaceSeparated(it, source("alloc"))) }

  private fun objCNameRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    mappedObjCNameRenderer(typeDeclaration) ?: nonMappedObjCNameRenderer(typeDeclaration)

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

  private fun nonMappedObjCNameRenderer(typeDeclaration: TypeDeclaration): Renderer<Source> =
    objCNameRenderer(typeDeclaration.kind, typeDeclaration.objCName(withPrefix = withJ2ktPrefix))

  private fun objCNameRenderer(kind: TypeDeclaration.Kind, name: String): Renderer<Source> =
    when (kind) {
      TypeDeclaration.Kind.CLASS,
      TypeDeclaration.Kind.ENUM -> className(name)
      TypeDeclaration.Kind.INTERFACE -> protocolName(name)
    }

  private fun objCRenderer(typeDescriptor: TypeDescriptor): Renderer<Source> =
    when (typeDescriptor) {
      is PrimitiveTypeDescriptor -> primitiveObjCRenderer(typeDescriptor)
      is DeclaredTypeDescriptor -> declaredObjCRenderer(typeDescriptor)
      // TODO: Handle TypeVariable and Array
      else -> id
    }.runIf(typeDescriptor.isNullable) { toNullable() }

  private fun primitiveObjCRenderer(
    primitiveTypeDescriptor: PrimitiveTypeDescriptor
  ): Renderer<Source> =
    when (primitiveTypeDescriptor) {
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

  private fun declaredObjCRenderer(
    declaredTypeDescriptor: DeclaredTypeDescriptor
  ): Renderer<Source> =
    when {
      isJavaLangObject(declaredTypeDescriptor) -> id
      declaredTypeDescriptor.isInterface -> interfaceObjCRenderer(declaredTypeDescriptor)
      else -> objCNameRenderer(declaredTypeDescriptor.typeDeclaration).map(::pointer)
    }

  private fun interfaceObjCRenderer(
    declaredTypeDescriptor: DeclaredTypeDescriptor
  ): Renderer<Source> =
    combine(id, objCNameRenderer(declaredTypeDescriptor.typeDeclaration)) { idSource, typeSource ->
      join(idSource, inAngleBrackets(typeSource))
    }

  private val j2ObjCTypesImport: Import
    get() = Import.local("third_party/java_src/j2objc/jre_emul/Classes/J2ObjC_types.h")

  private val j2ObjCTypesDependency: Dependency
    get() = Dependency.of(j2ObjCTypesImport)

  private fun j2ObjCTypeRenderer(name: String): Renderer<Source> =
    rendererOf(source(name)) + j2ObjCTypesDependency

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
    rendererOf(source(if (boolean) "true" else "false"))

  private fun literalRenderer(char: Char): Renderer<Source> =
    rendererOf(
      source(
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
    )

  private fun literalRenderer(byte: Byte): Renderer<Source> = rendererOf(source("$byte"))

  private fun literalRenderer(short: Short): Renderer<Source> = rendererOf(source("$short"))

  private fun literalRenderer(int: Int): Renderer<Source> =
    when (int) {
      Int.MIN_VALUE ->
        jintTypeRenderer.map {
          inParentheses(spaceSeparated(inParentheses(it), source("0x80000000")))
        }
      else -> rendererOf(source("$int"))
    }

  private fun literalRenderer(long: Long): Renderer<Source> =
    when (long) {
      Long.MIN_VALUE ->
        jlongTypeRenderer.map {
          inParentheses(spaceSeparated(inParentheses(it), source("0x8000000000000000LL")))
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
}
