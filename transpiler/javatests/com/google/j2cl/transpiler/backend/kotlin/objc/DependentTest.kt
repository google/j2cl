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

import com.google.common.truth.Truth.assertThat
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.dependent
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.flatten
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DependentTest {
  @Test
  fun dependent() {
    dependent("foo").assertGives("foo" to setOf())
  }

  @Test
  fun dependentPlus() {
    dependent("foo").with(DEPENDENCY_1).assertGives("foo" to setOf(DEPENDENCY_1))
    dependent("foo")
      .with(DEPENDENCY_1)
      .with(DEPENDENCY_2)
      .assertGives("foo" to setOf(DEPENDENCY_1, DEPENDENCY_2))
  }

  @Test
  fun dependentMap() {
    dependent("foo")
      .with(DEPENDENCY_1)
      .map { it.length }
      .assertGives("foo".length to setOf(DEPENDENCY_1))
  }

  @Test
  fun dependentBind() {
    dependent("foo")
      .with(DEPENDENCY_1)
      .bind { dependent(it.length) with DEPENDENCY_2 }
      .assertGives("foo".length to setOf(DEPENDENCY_1, DEPENDENCY_2))
  }

  @Test
  fun dependentCombine() {
    combine(dependent("foo") with DEPENDENCY_1, dependent("bar") with DEPENDENCY_2, String::plus)
      .assertGives("foobar" to setOf(DEPENDENCY_1, DEPENDENCY_2))

    combine(
        dependent("foo") with DEPENDENCY_1,
        dependent("bar") with DEPENDENCY_2,
        dependent("!") with DEPENDENCY_3,
      ) { a, b, c ->
        a + b + c
      }
      .assertGives("foobar!" to setOf(DEPENDENCY_1, DEPENDENCY_2, DEPENDENCY_3))

    combine(
        dependent("foo") with DEPENDENCY_1,
        dependent("bar") with DEPENDENCY_2,
        dependent("!") with DEPENDENCY_3,
        dependent("?") with DEPENDENCY_4,
      ) { a, b, c, d ->
        a + b + c + d
      }
      .assertGives("foobar!?" to setOf(DEPENDENCY_1, DEPENDENCY_2, DEPENDENCY_3, DEPENDENCY_4))
  }

  @Test
  fun dependentFlatten() {
    listOf(dependent("foo") with DEPENDENCY_1, dependent("bar") with DEPENDENCY_2)
      .flatten()
      .assertGives(listOf("foo", "bar") to setOf(DEPENDENCY_1, DEPENDENCY_2))
  }

  companion object {
    private val DEPENDENCY_1 = Dependency.of(Import.local("dependency_1.h"))
    private val DEPENDENCY_2 = Dependency.of(Import.local("dependency_2.h"))
    private val DEPENDENCY_3 = Dependency.of(Import.local("dependency_3.h"))
    private val DEPENDENCY_4 = Dependency.of(Import.local("dependency_4.h"))

    private fun <V> Dependent<V>.assertGives(valueToDependency: Pair<V, Set<Dependency>>) =
      assertThat(getWithDependencies()).isEqualTo(valueToDependency)
  }
}
