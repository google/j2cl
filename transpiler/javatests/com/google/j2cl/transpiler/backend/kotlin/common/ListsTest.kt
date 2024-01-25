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
class ListsTest {
  @Test
  fun listMapFirst() {
    assertThat(listOf<String>().mapFirst { it.uppercase() }).containsExactly()
    assertThat(listOf("a").mapFirst { it.uppercase() }).containsExactly("A").inOrder()
    assertThat(listOf("a", "b").mapFirst { it.uppercase() }).containsExactly("A", "b").inOrder()
  }
}
