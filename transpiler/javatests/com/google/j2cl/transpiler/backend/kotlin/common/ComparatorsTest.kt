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
package com.google.j2cl.transpiler.backend.kotlin.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComparatorsTest {
  @Test
  fun lexicalComparator() {
    assertThat(lexicalComparator.compare(listOf(1), listOf(2))).isLessThan(0)
    assertThat(lexicalComparator.compare(listOf(2), listOf(2))).isEqualTo(0)
    assertThat(lexicalComparator.compare(listOf(3), listOf(2))).isGreaterThan(0)

    assertThat(lexicalComparator.compare(listOf(1), listOf(1, 2))).isLessThan(0)
    assertThat(lexicalComparator.compare(listOf(1, 2), listOf(1, 2))).isEqualTo(0)
    assertThat(lexicalComparator.compare(listOf(1, 2, 3), listOf(1, 2))).isGreaterThan(0)
  }

  @Test
  fun lexicalComparator_largeInput_noStackOverflow() {
    assertThat(lexicalComparator.compare(1..1000000, 1..1000000)).isEqualTo(0)
  }

  private val lexicalComparator
    get() = Comparator.naturalOrder<Int>().lexical()
}
