/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.common;

import static com.google.common.truth.Truth.assertThat;
import static com.google.j2cl.common.StringUtils.capitalize;
import static com.google.j2cl.common.StringUtils.escapeAsWtf16;
import static com.google.j2cl.common.StringUtils.startsWithCamelCase;
import static com.google.j2cl.common.StringUtils.unescapeWtf16;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class StringUtilsTest {

  @Test
  public void testStartsWithCamelCase() {
    assertThat(startsWithCamelCase("getFoo", "get")).isTrue();
    assertThat(startsWithCamelCase("getFooBar", "getFoo")).isTrue();

    assertThat(startsWithCamelCase("get", "get")).isFalse();
    assertThat(startsWithCamelCase("getfoo", "get")).isFalse();
    assertThat(startsWithCamelCase("gotFoo", "get")).isFalse();
    assertThat(startsWithCamelCase("get_foo", "get")).isFalse();
    assertThat(startsWithCamelCase("get1Foo", "get")).isFalse();
    assertThat(startsWithCamelCase("get$Foo", "get")).isFalse();
    assertThat(startsWithCamelCase("", "get")).isFalse();
  }

  @Test
  public void testCapitalize() {
    assertThat(capitalize("foo")).isEqualTo("Foo");
    assertThat(capitalize("Foo")).isEqualTo("Foo");
    assertThat(capitalize("f")).isEqualTo("F");
    assertThat(capitalize("")).isEmpty();
  }

  @Test
  public void testEscapeAndUnescapeWtf16() {
    String testString = "Hello\tWorld\n\"Quotes\" and \\backslashes\\ and \u1234 unicode";
    String escaped = escapeAsWtf16(testString);
    assertThat(unescapeWtf16(escaped)).isEqualTo(testString);
  }
}
