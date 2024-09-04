/*
 * Copyright 2008 Google Inc.
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
package com.google.j2cl.jre.java8.util;

import java.util.Base64;
import junit.framework.TestCase;

/** Tests for Base64. */
public class Base64Test extends TestCase {
  // Encoded string of all possible byte values (0..255). This is used to make sure we can en- and
  // decode all byte values. The relevance of this is that the JS functions atob and btoa used for
  // the implementation encode from and to strings, so the character set used for the conversion
  // could mix codes if not set correctly when converting between byte arrays and strings.
  private static final String ENCODED_0_255 =
      "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+/w==";

  public void testDecode() {
    byte[] decoded = Base64.getDecoder().decode(ENCODED_0_255);
    assertEquals(256, decoded.length);
    for (int i = 0; i < decoded.length; i++) {
      assertEquals((byte) i, decoded[i]);
    }
  }

  public void testEncode() {
    byte[] original = new byte[256];
    for (int i = 0; i < original.length; i++) {
      original[i] = (byte) i;
    }
    String encoded = Base64.getEncoder().encodeToString(original);
    assertEquals(ENCODED_0_255, encoded);
  }
}
