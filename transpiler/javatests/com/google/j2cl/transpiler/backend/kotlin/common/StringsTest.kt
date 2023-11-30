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
class StringsTest {
  @Test
  fun camelCaseStartWithPrefix() {
    assertThat("getFoo".camelCaseStartsWith("get")).isTrue()
    assertThat("getFooBar".camelCaseStartsWith("getFoo")).isTrue()

    assertThat("get".camelCaseStartsWith("get")).isFalse()
    assertThat("getfoo".camelCaseStartsWith("get")).isFalse()
    assertThat("gotFoo".camelCaseStartsWith("get")).isFalse()
  }

  @Test
  fun stringTitleCased() {
    assertThat("foo".titleCased).isEqualTo("Foo")
    assertThat("1a".titleCased).isEqualTo("1a")
    assertThat("".titleCased).isEqualTo("")
  }

  @Test
  fun stringInBackTicks() {
    assertThat("foo".inBackTicks).isEqualTo("`foo`")
  }

  @Test
  fun stringInSingleQuotes() {
    assertThat("foo".inSingleQuotes).isEqualTo("'foo'")
  }

  @Test
  fun stringInDoubleQuotes() {
    assertThat("foo".inDoubleQuotes).isEqualTo("\"foo\"")
  }
}
