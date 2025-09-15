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

import static com.google.j2cl.jre.testing.TestUtils.isJvm;
import static com.google.j2cl.jre.testing.TestUtils.isWasm;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import junit.framework.TestCase;

public class PrintWriterTest extends TestCase {

  static class Bogus {
    @Override
    public String toString() {
      return "Bogus";
    }
  }

  private static class MockPrintWriter extends PrintWriter {

    public MockPrintWriter(OutputStream out, boolean autoflush) {
      super(out, autoflush);
    }

    @Override
    public void clearError() {
      super.clearError();
    }
  }

  private PrintWriter pw;
  private ByteArrayOutputStream bao;

  /** java.io.PrintWriter#PrintWriter(java.io.OutputStream) */
  public void testConstructor_withOutputStream() {
    // Test for method java.io.PrintWriter(java.io.OutputStream)
    pw.println("Random Chars");
    pw.write("Hello World");
    pw.flush();
    assertEquals("Random Chars\nHello World", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#PrintWriter(java.io.OutputStream, boolean) */
  public void testConstructor_withOutputStreamAndAutoFlush() {
    // Test for method java.io.PrintWriter(java.io.OutputStream, boolean)
    pw = new PrintWriter(bao, true);
    pw.println("Random Chars");
    pw.write("Hello World");
    if (isJvm()) {
      // JVM only. J2CL doesn't buffer so no partial output.
      assertEquals("Random Chars\n", bao.toString(UTF_8));
    }
    pw.flush();
    assertEquals("Random Chars\nHello World", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#PrintWriter(java.io.Writer) */
  public void testConstructor_withWriter() {
    // Test for method java.io.PrintWriter(java.io.Writer)
    StringWriter sw = new StringWriter();
    pw = new PrintWriter(sw);
    pw.print("Hello");
    pw.flush();
    assertEquals("Failed to construct proper writer", "Hello", sw.toString());
  }

  /** java.io.PrintWriter#PrintWriter(java.io.Writer, boolean) */
  public void testConstructor_withWriterAndAutoFlush() {
    // Test for method java.io.PrintWriter(java.io.Writer, boolean)
    StringWriter sw = new StringWriter();
    pw = new PrintWriter(sw, true);
    pw.print("Hello");
    // Auto-flush should have happened
    assertEquals("Failed to construct proper writer", "Hello", sw.toString());
  }

  /** java.io.PrintWriter#checkError() */
  public void testCheckError() {
    // Test for method boolean java.io.PrintWriter.checkError()
    pw.close();
    pw.print(490000000000.08765);
    assertTrue("Failed to return error", pw.checkError());
  }

  /**
   * java.io.PrintWriter#clearError()
   *
   * @since 1.6
   */
  public void testClearError() {
    // Test for method boolean java.io.PrintWriter.clearError()
    MockPrintWriter mpw = new MockPrintWriter(new ByteArrayOutputStream(), false);
    mpw.close();
    mpw.print(490000000000.08765);
    assertTrue("Failed to return error", mpw.checkError());
    mpw.clearError();
    assertFalse("Internal error state has not be cleared", mpw.checkError());
  }

  /** java.io.PrintWriter#close() */
  public void testClose() {
    // Test for method void java.io.PrintWriter.close()
    pw.close();
    pw.println("l");
    assertTrue("Write on closed stream failed to generate error", pw.checkError());
  }

  /** java.io.PrintWriter#flush() */
  public void testFlush() {
    // Test for method void java.io.PrintWriter.flush()
    final double dub = 490000000000.08765;
    pw.print(dub);
    pw.flush();
    assertEquals("Failed to flush", String.valueOf(dub), bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(char[]) */
  public void testPrint_charArray() {
    // Test for method void java.io.PrintWriter.print(char [])
    char[] schars = new char[11];
    "Hello World".getChars(0, 11, schars, 0);
    pw.print(schars);
    pw.flush();
    assertEquals("Hello World", bao.toString(UTF_8));

    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }
    assertThrows(NullPointerException.class, () -> pw.print((char[]) null));
  }

  /** java.io.PrintWriter#print(char) */
  public void testPrint_char() {
    // Test for method void java.io.PrintWriter.print(char)
    pw.print('c');
    pw.flush();
    assertEquals("Wrote incorrect char string", "c", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(double) */
  public void testPrint_double() {
    // Test for method void java.io.PrintWriter.print(double)
    final double dub = 490000000000.08765;
    pw.print(dub);
    pw.flush();
    assertEquals("Wrote incorrect double string", String.valueOf(dub), bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(float) */
  public void testPrint_float() {
    // Test for method void java.io.PrintWriter.print(float)
    final float flo = 49.08765f;
    pw.print(flo);
    pw.flush();
    assertEquals("Wrote incorrect float string", String.valueOf(flo), bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(int) */
  public void testPrint_int() {
    // Test for method void java.io.PrintWriter.print(int)
    pw.print(4908765);
    pw.flush();
    assertEquals("Wrote incorrect int string", "4908765", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(long) */
  public void testPrint_long() {
    // Test for method void java.io.PrintWriter.print(long)
    pw.print(49087650000L);
    pw.flush();
    assertEquals("Wrote incorrect long string", "49087650000", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(java.lang.Object) */
  public void testPrint_object() {
    // Test for method void java.io.PrintWriter.print(java.lang.Object)
    pw.print((Object) null);
    pw.flush();
    assertEquals("Did not write null", "null", bao.toString(UTF_8));
    bao.reset();

    pw.print(new Bogus());
    pw.flush();
    assertEquals("Wrote in incorrect Object string", "Bogus", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(java.lang.String) */
  public void testPrint_string() {
    // Test for method void java.io.PrintWriter.print(java.lang.String)
    pw.print((String) null);
    pw.flush();
    assertEquals("did not write null", "null", bao.toString(UTF_8));
    bao.reset();

    pw.print("Hello World");
    pw.flush();
    assertEquals("Wrote incorrect  string", "Hello World", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#print(boolean) */
  public void testPrint_boolean() {
    // Test for method void java.io.PrintWriter.print(boolean)
    pw.print(true);
    pw.flush();
    assertEquals("Wrote in incorrect boolean string", "true", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println() */
  public void testPrintln() {
    // Test for method void java.io.PrintWriter.println()
    pw.println("Blarg");
    pw.println();
    pw.println("Bleep");
    pw.flush();
    assertEquals("Blarg\n\nBleep\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(char[]) */
  public void testPrintln_charArray() {
    // Test for method void java.io.PrintWriter.println(char [])
    char[] schars = new char[11];
    "Hello World".getChars(0, 11, schars, 0);
    pw.println("Random Chars");
    pw.println(schars);
    pw.flush();
    assertEquals("Random Chars\nHello World\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(char) */
  public void testPrintln_char() {
    // Test for method void java.io.PrintWriter.println(char)
    pw.println("Random Chars");
    pw.println('c');
    pw.flush();
    assertEquals("Random Chars\nc\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(double) */
  public void testPrintln_double() {
    // Test for method void java.io.PrintWriter.println(double)
    final double dub = 400000.657483;
    pw.println("Random Chars");
    pw.println(dub);
    pw.flush();
    assertEquals("Random Chars\n400000.657483\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(float) */
  public void testPrintln_float() {
    // Test for method void java.io.PrintWriter.println(float)
    final float flo = 0.5f;
    pw.println("Random Chars");
    pw.println(flo);
    pw.flush();
    assertEquals("Random Chars\n0.5\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(int) */
  public void testPrintln_int() {
    // Test for method void java.io.PrintWriter.println(int)
    pw.println("Random Chars");
    pw.println(400000);
    pw.flush();
    assertEquals("Random Chars\n400000\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(long) */
  public void testPrintln_long() {
    // Test for method void java.io.PrintWriter.println(long)
    pw.println("Random Chars");
    pw.println(4000000000000L);
    pw.flush();
    assertEquals("Random Chars\n4000000000000\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(java.lang.Object) */
  public void testPrintln_object() {
    // Test for method void java.io.PrintWriter.println(java.lang.Object)
    pw.println("Random Chars");
    pw.println(new Bogus());
    pw.flush();
    assertEquals("Random Chars\nBogus\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(java.lang.String) */
  public void testPrintln_string() {
    // Test for method void java.io.PrintWriter.println(java.lang.String)
    pw.println("Random Chars");
    pw.println("Hello World");
    pw.flush();
    assertEquals("Random Chars\nHello World\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#println(boolean) */
  public void testPrintln_boolean() {
    // Test for method void java.io.PrintWriter.println(boolean)
    pw.println("Random Chars");
    pw.println(false);
    pw.flush();
    assertEquals("Random Chars\nfalse\n", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#write(char[]) */
  public void testWrite_charArray() {
    // Test for method void java.io.PrintWriter.write(char [])
    char[] schars = new char[11];
    "Hello World".getChars(0, 11, schars, 0);
    pw.println("Random Chars");
    pw.write(schars);
    pw.flush();
    assertEquals("Random Chars\nHello World", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#write(char[], int, int) */
  public void testWrite_charArrayWithOffsetAndLength() {
    // Test for method void java.io.PrintWriter.write(char [], int, int)
    char[] schars = new char[11];
    "Hello World".getChars(0, 11, schars, 0);
    pw.println("Random Chars");
    pw.write(schars, 6, 5);
    pw.flush();
    assertEquals("Random Chars\nWorld", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#write(int) */
  public void testWrite_int() throws IOException {
    // Test for method void java.io.PrintWriter.write(int)
    pw.write('a');
    pw.write('b');
    pw.write('c');
    pw.flush();
    byte[] rv = bao.toByteArray();
    assertTrue("Wrote incorrect ints", rv[0] == 'a' && rv[1] == 'b' && rv[2] == 'c');
  }

  /** java.io.PrintWriter#write(java.lang.String) */
  public void testWrite_string() {
    // Test for method void java.io.PrintWriter.write(java.lang.String)
    pw.println("Random Chars");
    pw.write("Hello World");
    pw.flush();
    assertEquals("Random Chars\nHello World", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#write(java.lang.String, int, int) */
  public void testWrite_stringWithOffsetAndLength() {
    // Test for method void java.io.PrintWriter.write(java.lang.String, int,
    // int)
    pw.println("Random Chars");
    pw.write("Hello World", 6, 5);
    pw.flush();
    assertEquals("Random Chars\nWorld", bao.toString(UTF_8));
  }

  /** java.io.PrintWriter#append(char) */
  public void testAppend_char() {
    char testChar = ' ';
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(out);
    printWriter.append(testChar);
    printWriter.flush();
    assertEquals(String.valueOf(testChar), out.toString(UTF_8));
    printWriter.close();
  }

  /** java.io.PrintWriter#append(CharSequence) */
  public void testAppend_charSequence() {
    String testString = "My Test String";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(out);
    printWriter.append(testString);
    printWriter.flush();
    assertEquals(testString, out.toString(UTF_8));
    printWriter.close();
  }

  /** java.io.PrintWriter#append(CharSequence, int, int) */
  public void testAppend_charSequenceWithStartAndEnd() {
    String testString = "My Test String";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(out);
    printWriter.append(testString, 1, 3);
    printWriter.flush();
    assertEquals(testString.substring(1, 3), out.toString(UTF_8));
    printWriter.close();
  }

  @Override
  protected void setUp() {
    bao = new ByteArrayOutputStream();
    pw = new PrintWriter(bao, false);
  }

  @Override
  protected void tearDown() {
    pw.close();
  }
}
