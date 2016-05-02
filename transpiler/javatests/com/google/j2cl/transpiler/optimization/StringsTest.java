package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
class StringsTest {

  @JsMethod
  public static boolean stringEqualsString() {
    return "".equals("");
  }

  @JsProperty
  private static native Object getStringEqualsString();

  @JsMethod
  public static boolean stringNotEqualsString() {
    return "".equals("asd");
  }

  @JsProperty
  private static native Object getStringNotEqualsString();

  @Test
  public void simpleEqualsOptimizes() {
    // TODO: Remove.
    // Prevent inlining of Equality.$same into String.equals.
    new Object().equals("sdsd");

    assertFunctionMatches(getStringEqualsString(), "return !0");
    assertFunctionMatches(getStringNotEqualsString(), "return !1");
  }

  @JsMethod
  public static boolean stringSameString() {
    return "" == "";
  }

  @JsProperty
  private static native Object getStringSameString();

  @Test
  public void simpleSameOptimizes() {
    assertFunctionMatches(getStringSameString(), "return !0");
  }
}
