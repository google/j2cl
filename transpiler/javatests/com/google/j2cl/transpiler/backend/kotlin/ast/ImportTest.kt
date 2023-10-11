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

import com.google.common.truth.Truth.assertThat
import com.google.j2cl.transpiler.backend.kotlin.ast.Import.Companion.lexicographicalOrder
import com.google.j2cl.transpiler.backend.kotlin.ast.Import.Suffix.WithAlias
import com.google.j2cl.transpiler.backend.kotlin.ast.Import.Suffix.WithStar
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImportTest {
  @Test
  fun importLexicographicalOrder() {
    assertThat(
        listOf(
            Import(listOf("foo")),
            Import(listOf("bar")),
            Import(listOf("foo", "bar")),
            Import(listOf("foo"), WithStar),
            Import(listOf("foo"), WithAlias("foo")),
            Import(listOf("foo"), WithAlias("bar"))
          )
          .sortedWith(lexicographicalOrder())
      )
      .containsExactly(
        Import(listOf("foo"), WithStar),
        Import(listOf("bar")),
        Import(listOf("foo")),
        Import(listOf("foo"), WithAlias("bar")),
        Import(listOf("foo"), WithAlias("foo")),
        Import(listOf("foo", "bar"))
      )
      .inOrder()
  }
}
