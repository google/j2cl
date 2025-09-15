/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.j2cl.jre.java.io;

import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import junit.framework.TestCase;

public class StringWriterTest extends TestCase {

  private StringWriter sw;

  /** java.io.StringWriter#StringWriter() */
  public void testConstructor() {
    // Test for method java.io.StringWriter()
    assertTrue("Used in tests", true);
  }

  /** java.io.StringWriter#close() */
  public void testClose() {
    // Test for method void java.io.StringWriter.close()
    try {
      sw.close();
    } catch (IOException e) {
      fail("IOException closing StringWriter : " + e.getMessage());
    }
  }

  /** java.io.StringWriter#flush() */
  public void testFlush() {
    // Test for method void java.io.StringWriter.flush()
    sw.flush();
    sw.write('c');
    assertEquals("Failed to flush char", "c", sw.toString());
  }

  /** java.io.StringWriter#getBuffer() */
  public void testGetBuffer() {
    // Test for method java.lang.StringBuffer
    // java.io.StringWriter.getBuffer()

    sw.write("This is a test string");
    String sb = sw.getBuffer().toString();
    assertEquals("Incorrect buffer returned", "This is a test string", sb);
  }

  /** java.io.StringWriter#toString() */
  public void testToString() {
    // Test for method java.lang.String java.io.StringWriter.toString()
    sw.write("This is a test string");
    assertEquals("Incorrect string returned", "This is a test string", sw.toString());
  }

  /** java.io.StringWriter#write(char[], int, int) */
  public void testWrite_charArrayWithOffsetAndLength() {
    // Test for method void java.io.StringWriter.write(char [], int, int)
    char[] c = new char[1000];
    "This is a test string".getChars(0, 21, c, 0);
    sw.write(c, 0, 21);
    assertEquals("Chars not written properly", "This is a test string", sw.toString());
  }

  /** java.io.StringWriter#write(char[], int, int) Regression for HARMONY-387 */
  public void testWrite_charArray_negativeLength_throwsException() {
    StringWriter obj = new StringWriter();
    assertThrows(IndexOutOfBoundsException.class, () -> obj.write(new char[0], 0, -1));
  }

  /** java.io.StringWriter#write(char[], int, int) */
  public void testWrite_charArray_negativeOffset_throwsException() {
    StringWriter obj = new StringWriter();
    assertThrows(IndexOutOfBoundsException.class, () -> obj.write(new char[0], -1, 0));
  }

  /** java.io.StringWriter#write(char[], int, int) */
  public void testWrite_charArray_negativeOffsetAndLength_throwsException() {
    StringWriter obj = new StringWriter();
    assertThrows(IndexOutOfBoundsException.class, () -> obj.write(new char[0], -1, -1));
  }

  /** java.io.StringWriter#write(int) */
  public void testWrite_int() {
    // Test for method void java.io.StringWriter.write(int)
    sw.write('c');
    assertEquals("Char not written properly", "c", sw.toString());
  }

  /** java.io.StringWriter#write(java.lang.String) */
  public void testWrite_string() {
    // Test for method void java.io.StringWriter.write(java.lang.String)
    sw.write("This is a test string");
    assertEquals("String not written properly", "This is a test string", sw.toString());
  }

  /** java.io.StringWriter#write(java.lang.String, int, int) */
  public void testWrite_stringWithOffsetAndLength() {
    // Test for method void java.io.StringWriter.write(java.lang.String,
    // int, int)
    sw.write("This is a test string", 2, 2);
    assertEquals("String not written properly", "is", sw.toString());
  }

  /** java.io.StringWriter#append(char) */
  public void testAppend_char() throws IOException {
    char testChar = ' ';
    StringWriter stringWriter = new StringWriter(20);
    stringWriter.append(testChar);
    assertEquals(String.valueOf(testChar), stringWriter.toString());
    stringWriter.close();
  }

  /** java.io.PrintWriter#append(CharSequence) */
  public void testAppend_charSequence() throws IOException {

    String testString = "My Test String";
    StringWriter stringWriter = new StringWriter(20);
    stringWriter.append(testString);
    assertEquals(String.valueOf(testString), stringWriter.toString());
    stringWriter.close();
  }

  /** java.io.PrintWriter#append(CharSequence, int, int) */
  public void testAppend_charSequenceWithStartAndEnd() throws IOException {
    String testString = "My Test String";
    StringWriter stringWriter = new StringWriter(20);
    stringWriter.append(testString, 1, 3);
    assertEquals(testString.substring(1, 3), stringWriter.toString());
    stringWriter.close();
  }

  @Override
  protected void setUp() {
    sw = new StringWriter();
  }
}
