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

import java.io.IOException;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Unit tests for the JS specific behavior of Throwable classes. */
@SuppressWarnings("ShouldNotSubclass")
public class JsThrowableTest extends JsThrowableTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCatchJava() {
    Throwable e = new Throwable();
    assertSame(e, catchJava(createThrower(e)));
  }

  public void testCatchNative() {
    Throwable e = new Throwable("<my msg>");
    Object caughtNative = catchNative(createThrower(e));
    assertTrue(caughtNative instanceof JsError);
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(Throwable.class.getName()));
  }

  public void testCatchNative_NullPointerException() {
    NullPointerException e = new NullPointerException("<my msg>");
    Object caughtNative = catchNative(createThrower(e));
    assertTrue(caughtNative instanceof TypeError);
    assertTrue(caughtNative.toString().startsWith("TypeError:"));
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(NullPointerException.class.getName()));
  }

  public void testCatchNative_fillInStackTraceOverride() {
    Throwable e = new Throwable("<my msg>") {
      public Throwable fillInStackTrace() {
        // Replace fill in stack trace with no-op.
        return this;
      }
    };

    Object caughtNative = catchNative(createThrower(e));
    assertTrue(caughtNative instanceof JsError);
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(e.getClass().getName()));
  }

  public void testCatchNativeWithNewlineInMesssage() {
    Throwable e = new Throwable("my\nmsg");
    Object caughtNative = catchNative(createThrower(e));
    assertTrue(caughtNative.toString().contains("my\nmsg"));
  }

  public void testJavaNativeJavaSandwichCatch() {
    Throwable e = new Throwable();
    assertSame(e, javaNativeJavaSandwich(e));
  }

  public void testLinkedBackingObjects() {
    Throwable rootCause = new Throwable("Root cause");
    Throwable subError = new Throwable("Sub-error", rootCause);

    assertEquals(getBackingJsObject(rootCause), getBackingJsObject(subError).getCause());
  }

  public void testLinkedBackingObjects_initCause() {
    Throwable rootCause = new Throwable("Root cause");
    Throwable subError = new Throwable("Sub-error");
    subError.initCause(rootCause);

    assertEquals(getBackingJsObject(rootCause), getBackingJsObject(subError).getCause());
  }

  public void testLinkedBackingObjects_noCause() {
    Throwable subError = new Throwable("Sub-error");

    assertNull(getBackingJsObject(subError).getCause());
  }

  public void testLinkedSuppressedErrors_suppressedAddedViaInit() {
    final Throwable suppressed = new Throwable();
    Throwable e =
        new Throwable() {
          {
            addSuppressed(suppressed);
          }
        };

    assertEquals(getBackingJsObject(suppressed), getBackingJsObject(e).getSuppressed()[0]);
  }

  public void testLinkedSuppressedErrors_tryWithResources() {
    class FailingResource implements AutoCloseable {
      @Override
      public void close() throws IOException {
        throw new IOException("onClose");
      }
    }

    RuntimeException e = new RuntimeException("try");
    try (FailingResource r = new FailingResource()) {
      throw e;
    } catch (Exception expected) { }

    assertEquals(
        getBackingJsObject(e.getSuppressed()[0]), getBackingJsObject(e).getSuppressed()[0]);
  }

  @JsType(isNative = true, name = "Error", namespace = JsPackage.GLOBAL)
  private static class JsError {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  private static class TypeError {}
}
