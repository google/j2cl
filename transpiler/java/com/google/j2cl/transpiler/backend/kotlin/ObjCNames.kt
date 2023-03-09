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

import com.google.j2cl.transpiler.backend.kotlin.common.camelCaseStartsWith
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.common.mapFirst
import com.google.j2cl.transpiler.backend.kotlin.common.titleCase

internal val String.escapeObjCKeyword
  get() = letIf(objCKeywords.contains(this)) { it + "_" }

internal fun String.escapeReservedObjCPrefixWith(newPrefix: String) =
  letIf(objCReservedPrefixes.any { camelCaseStartsWith(it) }) { "$newPrefix$titleCase" }

internal val String.escapeObjCProperty: String
  get() = escapeObjCKeyword.escapeReservedObjCPrefixWith("do")

internal val String.escapeObjCEnumProperty: String
  get() = escapeObjCKeyword.escapeReservedObjCPrefixWith("the")

internal fun MethodObjCNames.escapeObjCMethod(isConstructor: Boolean): MethodObjCNames =
  copy(
    methodName =
      methodName
        .letIf(parameterNames.isEmpty()) { it.escapeObjCKeyword }
        .letIf(!isConstructor) { it.escapeReservedObjCPrefixWith("do") },
    parameterNames = parameterNames.letIf(isConstructor) { it.mapFirst { "With$it" } }
  )

// Taken from GitHub:
// "JetBrains/kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/objcexport/ObjCExportNamer.kt"
private val objCReservedPrefixes = setOf("alloc", "copy", "mutableCopy", "new", "init")

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
    "xor_eq"
  )
