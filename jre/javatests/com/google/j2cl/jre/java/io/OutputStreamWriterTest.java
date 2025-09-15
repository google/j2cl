/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.jre.java.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import junit.framework.TestCase;

/** Unit test for the {@link java.io.OutputStreamWriter} emulated class. */
public class OutputStreamWriterTest extends TestCase {

  private final Charset encodingUTF8Charset = UTF_8;

  /** String containing unicode characters. */
  private static final String UNICODE_STRING = "ËÛëŶǾȜϞ";

  /** Array of characters that contains ASCII characters. */
  private static final char[] ASCII_CHAR_ARRAY = {'a', 'b', 'c', '"', '&', '<', '>'};

  /** {@link java.io.OutputStreamWriter} object being tested. */
  private OutputStreamWriter writer;

  /** Underlying output stream used by the {@link OutputStreamWriter} object. */
  private ByteArrayOutputStream baos;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    baos = new ByteArrayOutputStream();
    writer = new OutputStreamWriter(baos, encodingUTF8Charset);
  }

  public void testNullCharset() throws UnsupportedEncodingException {
    Charset nullCharset = null;
    try {
      new OutputStreamWriter(baos, nullCharset);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testNullOutputStream() throws UnsupportedEncodingException {
    try {
      new OutputStreamWriter(/* out= */ null, encodingUTF8Charset);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testWriteUnicodeChar() throws IOException {
    writer.write(UNICODE_STRING, 0, UNICODE_STRING.length());
    writer.close();
    assertTrue(Arrays.equals(UNICODE_STRING.getBytes(encodingUTF8Charset), baos.toByteArray()));
  }

  public void testWriteASCIIChar() throws IOException {
    writer.write(ASCII_CHAR_ARRAY, 0, ASCII_CHAR_ARRAY.length);
    writer.close();
    assertTrue(
        Arrays.equals(
            new String(ASCII_CHAR_ARRAY).getBytes(encodingUTF8Charset), baos.toByteArray()));
  }

  public void testWriteArrayUsingNullArray() throws IOException {
    final char[] b = null;
    try {
      writer.write(b, 0, 2);
      fail("should have thrown NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testWriteArrayUsingNegativeOffsetValue() throws IOException {
    final char[] b = {'a', 'b'};
    try {
      writer.write(b, -1, 1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testWriteArrayUsingNegativeLengthValue() throws IOException {
    final char[] b = {'a', 'b'};
    try {
      writer.write(b, 0, -1);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testWriteArrayUsingInvalidRangeValue() throws IOException {
    final char[] b = {'a', 'b'};
    try {
      writer.write(b, 1, 2);
      fail("should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testWriteArrayInvalidCodepoint() throws IOException {
    char[] chars = {'\uFFFF'};
    writer.write(chars);
    writer.close();
    assertTrue(
        Arrays.equals(new byte[] {(byte) 0xEF, (byte) 0xBF, (byte) 0xBF}, baos.toByteArray()));
  }

  public void testWriteArrayHighSurrogateAtEnd() throws IOException {
    char[] chars = {'\uD801'};
    writer.write(chars);
    writer.close();
    assertTrue(Arrays.equals(new byte[] {'?'}, baos.toByteArray()));
  }

  public void testWriteArrayHighSurrogateWithoutLowSurrogate() throws IOException {
    char[] chars = {'\uD801', ' '};
    writer.write(chars);
    writer.close();
    assertTrue(Arrays.equals(new byte[] {'?', ' '}, baos.toByteArray()));
  }

  public void testWriteArrayLowSurrogateWithoutHighSurrogate() throws IOException {
    char[] chars = {' ', '\uDDCB'};
    writer.write(chars);
    writer.close();
    assertTrue(Arrays.equals(new byte[] {' ', '?'}, baos.toByteArray()));
  }

  public void testWriteArraySurrogatesReversed() throws IOException {
    char[] chars = {'\uDDCB', '\uDDCB'};
    writer.write(chars);
    writer.close();
    assertTrue(Arrays.equals(new byte[] {'?', '?'}, baos.toByteArray()));
  }

  public void testWriteHighAndLowSurrogateSeparately() throws IOException {
    char[] chars = {'\ud83c', '\udf09'};
    writer.write(chars, 0, 1);
    writer.write(chars, 1, 1);
    writer.close();
    assertEquals("\ud83c\udf09", new String(baos.toByteArray(), encodingUTF8Charset));
  }
}
