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
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.flatten
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.rendererOf
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RendererTest {
  @Test
  fun rendererOf() {
    rendererOf("foo").assertRenders("foo" to setOf())
  }

  @Test
  fun rendererPlus() {
    rendererOf("foo").plus(DEPENDENCY_1).assertRenders("foo" to setOf(DEPENDENCY_1))
    rendererOf("foo")
      .plus(DEPENDENCY_1)
      .plus(DEPENDENCY_2)
      .assertRenders("foo" to setOf(DEPENDENCY_1, DEPENDENCY_2))
  }

  @Test
  fun rendererMap() {
    rendererOf("foo")
      .plus(DEPENDENCY_1)
      .map { it.length }
      .assertRenders("foo".length to setOf(DEPENDENCY_1))
  }

  @Test
  fun rendererBind() {
    rendererOf("foo")
      .plus(DEPENDENCY_1)
      .bind { rendererOf(it.length).plus(DEPENDENCY_2) }
      .assertRenders("foo".length to setOf(DEPENDENCY_1, DEPENDENCY_2))
  }

  @Test
  fun rendererCombine() {
    combine(
        rendererOf("foo").plus(DEPENDENCY_1),
        rendererOf("bar").plus(DEPENDENCY_2),
        String::plus
      )
      .assertRenders("foobar" to setOf(DEPENDENCY_1, DEPENDENCY_2))

    combine(
        rendererOf("foo").plus(DEPENDENCY_1),
        rendererOf("bar").plus(DEPENDENCY_2),
        rendererOf("!").plus(DEPENDENCY_3)
      ) { a, b, c ->
        a + b + c
      }
      .assertRenders("foobar!" to setOf(DEPENDENCY_1, DEPENDENCY_2, DEPENDENCY_3))

    combine(
        rendererOf("foo").plus(DEPENDENCY_1),
        rendererOf("bar").plus(DEPENDENCY_2),
        rendererOf("!").plus(DEPENDENCY_3),
        rendererOf("?").plus(DEPENDENCY_4)
      ) { a, b, c, d ->
        a + b + c + d
      }
      .assertRenders("foobar!?" to setOf(DEPENDENCY_1, DEPENDENCY_2, DEPENDENCY_3, DEPENDENCY_4))
  }

  @Test
  fun rendererFlatten() {
    listOf(rendererOf("foo").plus(DEPENDENCY_1), rendererOf("bar").plus(DEPENDENCY_2))
      .flatten()
      .assertRenders(listOf("foo", "bar") to setOf(DEPENDENCY_1, DEPENDENCY_2))
  }

  companion object {
    private val DEPENDENCY_1 = Dependency.of(Import.local("dependency_1.h"))
    private val DEPENDENCY_2 = Dependency.of(Import.local("dependency_2.h"))
    private val DEPENDENCY_3 = Dependency.of(Import.local("dependency_3.h"))
    private val DEPENDENCY_4 = Dependency.of(Import.local("dependency_4.h"))

    private fun <V> Renderer<V>.assertRenders(valueToDependency: Pair<V, Set<Dependency>>) =
      assertThat(renderWithDependencies()).isEqualTo(valueToDependency)
  }
}
