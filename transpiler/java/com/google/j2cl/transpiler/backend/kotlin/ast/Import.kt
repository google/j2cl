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

import com.google.common.collect.Comparators.lexicographical

/** J2KT representation of Kotlin import. */
data class Import(
  /** The list of path components. */
  val pathComponents: List<String>,

  /** The optional suffix. */
  val suffixOrNull: Suffix? = null,
) {
  /** Returns true if this is a star import. */
  private val isStar: Boolean
    get() = suffixOrNull is Suffix.WithStar

  /** Returns this import's alias, or null if absent. */
  private val aliasOrNull: String?
    get() = suffixOrNull?.let { it as? Suffix.WithAlias }?.alias

  companion object {
    /** Returns import with the given components on its path. */
    fun import(vararg components: String) = Import(components.toList())

    /** Returns star import with the given components on its path. */
    fun starImport(vararg components: String) =
      Import(components.toList(), suffixOrNull = Suffix.WithStar)

    /** Returns lexicographical comparator: star imports first, then by path, then by alias. */
    fun lexicographicalOrder(): Comparator<Import> = LEXICOGRAPHICAL_COMPARATOR

    private val LEXICOGRAPHICAL_COMPARATOR =
      compareBy<Import, Int>(naturalOrder()) { if (it.isStar) 0 else 1 }
        .then(compareBy(lexicographical(naturalOrder<String>())) { it.pathComponents })
        .then(compareBy(nullsFirst(naturalOrder<String>())) { it.aliasOrNull })
  }

  /** Import suffix. */
  sealed class Suffix {
    /** Import suffix with a star. */
    object WithStar : Suffix()

    /** Import suffix with an alias. */
    data class WithAlias(val alias: String) : Suffix()
  }
}
