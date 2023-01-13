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
import com.google.j2cl.transpiler.backend.kotlin.source.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inDoubleQuotes
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

data class Import(val path: String, val isLocal: Boolean)

fun systemImport(path: String) = Import(path, isLocal = false)

fun localImport(path: String) = Import(path, isLocal = true)

val Import.source
  get() =
    spaceSeparated(
      source("#import"),
      source(path).let { if (isLocal) inDoubleQuotes(it) else inAngleBrackets(it) }
    )

fun source(imports: List<Import>) =
  emptyLineSeparated(
    newLineSeparated(imports.filter { !it.isLocal }.sortedBy { it.path }.map { it.source }),
    newLineSeparated(imports.filter { it.isLocal }.sortedBy { it.path }.map { it.source })
  )
