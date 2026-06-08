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
package com.google.j2cl.transpiler.backend.kotlin.ast

import com.google.j2cl.transpiler.ast.Visibility as JavaVisibility

/** Kotlin visibility. */
enum class Visibility {
  PUBLIC,
  PROTECTED,
  INTERNAL,
  PRIVATE;

  val isPublic
    get() = this === PUBLIC

  val isProtected
    get() = this === PROTECTED

  val isInternal
    get() = this === INTERNAL

  val isPrivate
    get() = this === PRIVATE

  fun hasWiderScopeThan(other: Visibility): Boolean = ordinal < other.ordinal

  fun hasNarrowerScopeThan(other: Visibility): Boolean = ordinal > other.ordinal

  companion object {
    fun from(javaVisibility: JavaVisibility): Visibility =
      when (javaVisibility) {
        JavaVisibility.PUBLIC -> PUBLIC
        JavaVisibility.PROTECTED -> PROTECTED
        JavaVisibility.PACKAGE_PRIVATE -> INTERNAL
        JavaVisibility.PRIVATE -> PRIVATE
      }
  }
}

fun Visibility.widenUp(other: Visibility?) = if (other == null) this else minOf(this, other)

fun Visibility.narrowDown(other: Visibility?) = if (other == null) this else maxOf(this, other)

/** Returns visibility with the widest scope of [visibilities] or null if none were given. */
fun Iterable<Visibility>.withWidestScopeOrNull(): Visibility? = minOrNull()

/** Returns visibility with the narrowest scope of [visibilities] or null if none were given. */
fun Iterable<Visibility>.withNarrowestScopeOrNull(): Visibility? = maxOrNull()

fun Visibility.widenUp(others: Iterable<Visibility?>) =
  others.fold(this) { acc, element -> acc.widenUp(element) }

fun Visibility.narrowDown(others: Iterable<Visibility?>) =
  others.fold(this) { acc, element -> acc.narrowDown(element) }
