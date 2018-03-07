package com.google.j2cl.junit.integration.stacktrace.data;

import jsinterop.annotations.JsMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test for stacktraces out of native methods. */
@RunWith(JUnit4.class)
public class ThrowsInNativeJs {

  @Test
  public void test() {
    throwsInNative();
  }

  @JsMethod
  public static native void throwsInNative();
}
