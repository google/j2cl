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

import com.google.j2cl.transpiler.backend.kotlin.common.lexical

/** J2KT representation of Kotlin import. */
data class Import(
  /** The list of path components. */
  val pathComponents: List<String>,

  /** The optional suffix. */
  val suffixOrNull: Suffix? = null
) : Comparable<Import> {
  override fun compareTo(other: Import) = comparator.compare(this, other)

  companion object {
    /** Returns import with the given components on its path. */
    fun import(vararg components: String) = Import(components.toList())

    /** Returns star import with the given components on its path. */
    fun starImport(vararg components: String) =
      Import(components.toList(), suffixOrNull = Suffix.WithStar)

    /** Default import comparator. */
    private val comparator =
      compareBy<Import> { it.suffixOrNull == Suffix.WithStar }
        .reversed()
        .then(
          compareBy<Import, List<String>>(naturalOrder<String>().lexical()) {
            it.pathComponents.run {
              if (it.suffixOrNull is Suffix.WithAlias) plus(it.suffixOrNull.alias) else this
            }
          }
        )
  }

  /** Import suffix. */
  sealed class Suffix {
    /** Import suffix with a star. */
    object WithStar : Suffix()

    /** Import suffix with an alias. */
    data class WithAlias(val alias: String) : Suffix()
  }
}
