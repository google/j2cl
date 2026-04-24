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
package com.google.j2cl.transpiler.frontend.common;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class Utf8ByteOffsetConverterTest {

  @Test
  public void testAsciiOnly() {
    String text = "Hello, World!";
    Utf8ByteOffsetConverter converter = Utf8ByteOffsetConverter.create(text);

    // All byte offsets should be equal to the character offset.
    for (int i = 0; i <= text.length(); i++) {
      assertThat(converter.getByteOffset(i)).isEqualTo(i);
    }
  }

  @Test
  public void testEmptyString() {
    String text = "";
    Utf8ByteOffsetConverter converter = Utf8ByteOffsetConverter.create(text);

    assertThat(converter.getByteOffset(0)).isEqualTo(0);
  }

  @Test
  public void testMixedMultiByteCharacters() {
    // 'a' (1 byte)
    // '©' (2 bytes)
    // 'b' (1 byte)
    // '語' (3 bytes)
    // 'c' (1 byte)
    // '𐍈' (surrogate pair, 4 bytes)
    String text = "a©b語c𐍈";
    Utf8ByteOffsetConverter converter = Utf8ByteOffsetConverter.create(text);

    assertThat(converter.getByteOffset(0)).isEqualTo(0);
    assertThat(converter.getByteOffset(1)).isEqualTo(1);
    assertThat(converter.getByteOffset(2)).isEqualTo(3);
    assertThat(converter.getByteOffset(3)).isEqualTo(4);
    assertThat(converter.getByteOffset(4)).isEqualTo(7);
    assertThat(converter.getByteOffset(5)).isEqualTo(8);
    assertThat(converter.getByteOffset(6)).isEqualTo(10); // split surrogate pair
    assertThat(converter.getByteOffset(7)).isEqualTo(12);
  }

  @Test
  public void testConsecutiveMultiByteCharacters() {
    String text = "©©©©©";
    Utf8ByteOffsetConverter converter = Utf8ByteOffsetConverter.create(text);

    for (int i = 0; i <= text.length(); i++) {
      int expectedByteOffset = text.substring(0, i).getBytes(UTF_8).length;
      assertThat(converter.getByteOffset(i)).isEqualTo(expectedByteOffset);
    }
  }
}
