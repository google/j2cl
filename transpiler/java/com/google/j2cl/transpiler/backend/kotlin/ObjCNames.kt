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

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.FieldDescriptor
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeVariable
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor
import com.google.j2cl.transpiler.backend.kotlin.ast.CompanionDeclaration
import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility as KtVisibility
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.mapFirst
import com.google.j2cl.transpiler.backend.kotlin.common.titleCased

/** The names are mangled according to J2ObjC rules. */
internal data class MethodObjCNames(val objCName: ObjCName, val parameterObjCNames: List<ObjCName>)

/** ObjC name, together with its Swift counterpart. */
internal data class ObjCName(val string: String, val swiftString: String? = null)

internal val String.escapeObjCKeyword
  get() = letIf(objCKeywords.contains(this)) { it + "_" }

internal val KtVisibility.needsObjCNameAnnotation
  get() = isPublic || isProtected

internal fun MethodDescriptor.toObjCNames(): MethodObjCNames? =
  when {
    isConstructor -> toConstructorObjCNames()
    else -> toNonConstructorObjCNames()
  }

internal fun MethodDescriptor.toConstructorObjCNames(): MethodObjCNames =
  objectiveCName.let { objectiveCName ->
    MethodObjCNames(
      ObjCName(string = "init"),
      if (
          objectiveCName != null &&
            (objectiveCName.contains(":") || objectiveCName.startsWith("initWith"))
        ) {
          objectiveCName.objCMethodParameterNames.mapFirst {
            val prefix = "initWith"
            if (it.startsWith(prefix)) {
              it.substring(prefix.length)
            } else {
              parameterTypeDescriptors.first().parameterObjCName
            }
          }
        } else {
          parameterTypeDescriptors.mapIndexed { index, typeDescriptor ->
            typeDescriptor.parameterObjCName.letIf(index != 0) { "with$it" }
          }
        }
        .map { ObjCName(string = it) },
    )
  }

internal fun MethodDescriptor.toNonConstructorObjCNames(): MethodObjCNames =
  objectiveCName.let { objectiveCName ->
    if (objectiveCName == null || !objectiveCName.contains(":")) {
      MethodObjCNames(
        ObjCName(string = objectiveCName ?: ktName.escapeJ2ObjCKeyword, swiftString = swiftName),
        parameterTypeDescriptors.map {
          ObjCName(string = "with${it.parameterObjCName}", swiftString = swiftParameterName(it))
        },
      )
    } else {
      val objCParameterNames = objectiveCName.objCMethodParameterNames
      val firstObjCParameterName = objCParameterNames.first()
      // Split string by one of:
      // - first occurrence of "With",
      // - index of last uppercase character,
      // - in half arbitrarily.
      val splitIndex =
        null
          ?: firstObjCParameterName.indexOf("With").takeIf { it > 0 }
          ?: firstObjCParameterName.indexOfLast { it.isUpperCase() }.takeIf { it > 0 }
          ?: firstObjCParameterName.length
      MethodObjCNames(
        ObjCName(string = firstObjCParameterName.substring(0, splitIndex)),
        objCParameterNames.mapFirst { it.substring(splitIndex) }.map { ObjCName(string = it) },
      )
    }
  }

private val String.objCMethodParameterNames: List<String>
  get() = letIf(lastOrNull() == ':') { dropLast(1) }.split(":")

internal fun TypeDeclaration.objCName(prefix: String): String = prefix + objCNameWithoutPrefix

internal val TypeDeclaration.objCNameWithoutPrefix: String
  get() = mappedObjCName ?: nonMappedObjCName

private val String.objCCompanionTypeName: String
  get() = this + "Companion"

internal fun CompanionDeclaration.objCName(prefix: String) =
  enclosingTypeDeclaration.objCName(prefix).objCCompanionTypeName

internal val CompanionDeclaration.objCNameWithoutPrefix
  get() = enclosingTypeDeclaration.objCNameWithoutPrefix.objCCompanionTypeName

private val TypeDeclaration.nonMappedObjCName: String
  get() = objectiveCName ?: defaultObjCName

private val TypeDeclaration.mappedObjCName: String?
  get() =
    when (qualifiedBinaryName) {
      "java.lang.Object" -> "NSObject"
      "java.lang.String" -> "NSString"
      "java.lang.Class" -> "IOSClass"
      "java.lang.Number" -> "NSNumber"
      "java.lang.Cloneable" -> "NSCopying"
      else -> null
    }

private val TypeDeclaration.defaultObjCName: String
  get() = objCNamePrefix + simpleObjCName

private val TypeDeclaration.objCNamePrefix: String
  get() =
    enclosingTypeDeclaration.run {
      if (this != null) {
        objCNameWithoutPrefix + "_"
      } else {
        simpleObjCNamePrefix
      }
    }

private val TypeDeclaration.simpleObjCNamePrefix: String
  get() = objectiveCNamePrefix ?: objCPackagePrefix

private val TypeDeclaration.simpleObjCName: String
  get() = simpleSourceName.objCName

private val TypeDeclaration.objCPackagePrefix: String
  get() = packageName?.objCPackagePrefix ?: ""

internal val String.objCPackagePrefix: String
  get() = qualifiedNameToObjCName

internal val String.qualifiedNameToObjCName: String
  get() = split('.').joinToString(separator = "") { it.titleCased.objCName }

internal val String.objCName
  get() = replace('$', '_')

private const val ID_OBJC_NAME = "id"

internal fun TypeDescriptor.objCName(useId: Boolean): String =
  when (this) {
    is PrimitiveTypeDescriptor -> primitiveObjCName
    is ArrayTypeDescriptor -> arrayObjCName
    is DeclaredTypeDescriptor -> declaredObjCName(useId = useId)
    is TypeVariable -> variableObjCName(useId = useId)
    is IntersectionTypeDescriptor -> intersectionObjCName(useId = useId)
    is UnionTypeDescriptor -> ID_OBJC_NAME
  }

private val PrimitiveTypeDescriptor.primitiveObjCName: String
  get() =
    when (this) {
      PrimitiveTypes.VOID -> "void"
      PrimitiveTypes.BOOLEAN -> "boolean"
      PrimitiveTypes.BYTE -> "byte"
      PrimitiveTypes.SHORT -> "short"
      PrimitiveTypes.INT -> "int"
      PrimitiveTypes.LONG -> "long"
      PrimitiveTypes.CHAR -> "char"
      PrimitiveTypes.FLOAT -> "float"
      PrimitiveTypes.DOUBLE -> "double"
      else -> throw InternalCompilerError("Unexpected ${this::class.java.simpleName}")
    }

private fun DeclaredTypeDescriptor.declaredObjCName(useId: Boolean): String =
  if (useId && TypeDescriptors.isJavaLangObject(this)) {
    ID_OBJC_NAME
  } else {
    typeDeclaration.objCNameWithoutPrefix
  }

private val ArrayTypeDescriptor.arrayObjCName: String
  get() = leafTypeDescriptor.objCName(useId = false) + "Array" + dimensionsSuffix

private val ArrayTypeDescriptor.dimensionsSuffix: String
  get() = if (dimensions > 1) "$dimensions" else ""

private fun TypeVariable.variableObjCName(useId: Boolean): String =
  upperBoundTypeDescriptor.objCName(useId = useId)

private fun IntersectionTypeDescriptor.intersectionObjCName(useId: Boolean): String =
  firstType.objCName(useId = useId)

internal val TypeDescriptor.parameterObjCName: String
  get() = objCName(useId = true).titleCased

internal val FieldDescriptor.objCName: String
  get() = name!!.objCName.escapeJ2ObjCKeyword.letIf(objCNameNeedsUnderscoreSuffix) { it + "_" }

// Enum values and static final fields should follow naming convention and use SNAKE_CASE, so they
// should not conflict with methods. Other fields require underscore suffix to avoid naming
// conflict.
private val FieldDescriptor.objCNameNeedsUnderscoreSuffix: Boolean
  get() = !isEnumConstant && (!isStatic || !isFinal)

internal fun MethodObjCNames.escapeObjCMethod(isConstructor: Boolean): MethodObjCNames =
  copy(
    objCName =
      ObjCName(
        string = objCName.string.letIf(parameterObjCNames.isEmpty()) { it.escapeObjCKeyword }
      ),
    parameterObjCNames =
      parameterObjCNames.letIf(isConstructor) {
        it.mapFirst { ObjCName(string = "With${it.string}") }
      },
  )

// Taken from GitHub:
// "JetBrains/kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/CAdapterGenerator.kt"
private val objCKeywords =
  setOf(
    // Actual C keywords.
    "auto",
    "break",
    "case",
    "char",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extern",
    "float",
    "for",
    "goto",
    "if",
    "int",
    "long",
    "register",
    "return",
    "short",
    "signed",
    "sizeof",
    "static",
    "struct",
    "switch",
    "typedef",
    "union",
    "unsigned",
    "void",
    "volatile",
    "while",
    // C99-specific.
    "_Bool",
    "_Complex",
    "_Imaginary",
    "inline",
    "restrict",
    // C11-specific.
    "_Alignas",
    "_Alignof",
    "_Atomic",
    "_Generic",
    "_Noreturn",
    "_Static_assert",
    "_Thread_local",
    // Not exactly keywords, but reserved or standard-defined.
    "id",
    "and",
    "not",
    "or",
    "xor",
    "bool",
    "complex",
    "imaginary",

    // C++ keywords not listed above.
    "alignas",
    "alignof",
    "and_eq",
    "asm",
    "bitand",
    "bitor",
    "bool",
    "catch",
    "char16_t",
    "char32_t",
    "class",
    "compl",
    "constexpr",
    "const_cast",
    "decltype",
    "delete",
    "dynamic_cast",
    "explicit",
    "export",
    "false",
    "friend",
    "inline",
    "mutable",
    "namespace",
    "new",
    "noexcept",
    "not_eq",
    "nullptr",
    "operator",
    "or_eq",
    "private",
    "protected",
    "public",
    "reinterpret_cast",
    "static_assert",
    "template",
    "this",
    "thread_local",
    "throw",
    "true",
    "try",
    "typeid",
    "typename",
    "using",
    "virtual",
    "wchar_t",
    "xor_eq",
  )
