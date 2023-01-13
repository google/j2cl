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
package com.google.j2cl.transpiler.backend.kotlin.objc

import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

/** Forward declaration: @class or @protocol. */
data class ForwardDeclaration(val kind: Kind, val name: String) {
  enum class Kind {
    CLASS,
    PROTOCOL
  }
}

fun classForwardDeclaration(name: String) = ForwardDeclaration(ForwardDeclaration.Kind.CLASS, name)

fun protocolForwardDeclaration(name: String) =
  ForwardDeclaration(ForwardDeclaration.Kind.PROTOCOL, name)

val ForwardDeclaration.source
  get() = semicolonDeclaration(spaceSeparated(kind.source, source(name)))

val ForwardDeclaration.Kind.source
  get() = source("@$objCName")

val ForwardDeclaration.Kind.objCName
  get() =
    when (this) {
      ForwardDeclaration.Kind.CLASS -> "class"
      ForwardDeclaration.Kind.PROTOCOL -> "protocol"
    }

fun source(forwardDeclarations: List<ForwardDeclaration>) =
  emptyLineSeparated(
    forwardDeclarations
      .groupBy { it.kind }
      .entries
      .sortedBy { it.key }
      .map { it.value.sortedBy(ForwardDeclaration::name) }
      .map { newLineSeparated(it.map(ForwardDeclaration::source)) }
  )

val ForwardDeclaration.nameRendering
  get() = source(name) renderingWith dependency(this)
