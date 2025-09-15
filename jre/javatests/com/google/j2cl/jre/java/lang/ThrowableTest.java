/*
 * Copyright 2013 Google Inc.
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
package com.google.j2cl.jre.java.lang;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import junit.framework.TestCase;

/** Unit tests for the GWT emulation of java.lang.Throwable class. */
public class ThrowableTest extends TestCase {

  @J2ktIncompatible // Currently unsupported
  public static void testStackTrace() {
    Throwable e = new Throwable("<my msg>");
    assertTrue(e.getStackTrace().length > 0);

    e =
        new Throwable("<my msg>") {
          public Throwable fillInStackTrace() {
            // Replace fill in stack trace with no-op.
            return this;
          }
        };
    assertEquals(0, e.getStackTrace().length);

    e = new Throwable("<my msg>", null, true, false) {};
    assertEquals(0, e.getStackTrace().length);
  }

  @J2ktIncompatible // Currently unsupported
  public void testSetStackTrace() {
    Throwable throwable = new Throwable("stacktrace");
    throwable.fillInStackTrace();
    StackTraceElement[] newStackTrace = new StackTraceElement[2];
    newStackTrace[0] = new StackTraceElement("TestClass", "testMethod", "fakefile", 10);
    newStackTrace[1] = new StackTraceElement("TestClass", "testCaller", "fakefile2", 97);
    throwable.setStackTrace(newStackTrace);
    StackTraceElement[] trace = throwable.getStackTrace();
    assertNotNull(trace);
    assertEquals(2, trace.length);
    assertEquals("TestClass", trace[0].getClassName());
    assertEquals("testMethod", trace[0].getMethodName());
    assertEquals("fakefile", trace[0].getFileName());
    assertEquals(10, trace[0].getLineNumber());
    assertEquals("TestClass.testMethod(fakefile:10)", trace[0].toString());
    assertEquals("TestClass.testCaller(fakefile2:97)", trace[1].toString());
  }

  public void testExceptionToString() {
    Throwable inner = new RuntimeException("inner");
    assertEquals("outer", new RuntimeException("outer", inner).getMessage());

    // Prefixed with the class name, but is obfuscated for j2wasm, so we just check for the
    // ":" after the class name before the inner message.
    assertTrue(new RuntimeException(inner).getMessage().endsWith(": inner"));
  }
}
