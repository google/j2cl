package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
class BooleansTest {

  @JsMethod
  public static boolean simpleComp() {
    return true == true;
  }

  @JsProperty
  private static native Object getSimpleComp();

  @Test
  public void simpleCompOptimizes() {
    assertFunctionMatches(getSimpleComp(), "return !0");
  }
}
