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
package com.google.j2cl.transpiler.backend.kotlin.ast

import com.google.j2cl.transpiler.backend.kotlin.common.ofList

data class Import(val components: List<String>, val isStar: Boolean = false) : Comparable<Import> {
  override fun compareTo(other: Import) = comparator.compare(this, other)

  companion object {
    private val comparator =
      compareBy(Import::isStar)
        .reversed()
        .then(compareBy(naturalOrder<String>().ofList(), Import::components))
  }
}

fun import(vararg components: String) = Import(components.toList())

fun starImport(vararg components: String) = Import(components.toList(), isStar = true)

val defaultImports: Set<Import>
  get() = setOf(starImport("javaemul", "lang"))
