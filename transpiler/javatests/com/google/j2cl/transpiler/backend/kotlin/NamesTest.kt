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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NamesTest {
  @Test
  fun stringQualifiedNameComponents() {
    assertThat("Foo".qualifiedNameComponents()).containsExactly("Foo").inOrder()
    assertThat("Foo.Bar".qualifiedNameComponents()).containsExactly("Foo", "Bar").inOrder()
    assertThat("Foo.Bar.Zoo".qualifiedNameComponents())
      .containsExactly("Foo", "Bar", "Zoo")
      .inOrder()
  }

  @Test
  fun stringQualifiedNameToSimpleName() {
    assertThat("Foo".qualifiedNameToSimpleName()).isEqualTo("Foo")
    assertThat("Foo.Bar".qualifiedNameToSimpleName()).isEqualTo("Bar")
    assertThat("Foo.Bar.Zoo".qualifiedNameToSimpleName()).isEqualTo("Zoo")
  }

  @Test
  fun stringQualifiedNameToAlias() {
    assertThat("Foo".qualifiedNameToAlias()).isEqualTo("Foo")
    assertThat("Foo.Bar".qualifiedNameToAlias()).isEqualTo("Foo_Bar")
    assertThat("Foo.Bar.Zoo".qualifiedNameToAlias()).isEqualTo("Foo_Bar_Zoo")
  }
}
