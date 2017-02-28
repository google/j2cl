package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
class BooleansTest {

  @JsMethod
  public boolean simpleComp() {
    return true == true;
  }

  @Test
  public void simpleCompOptimizes() {
    assertFunctionMatches(((MethodsAsProperties) this).getSimpleComp(), "return !0;");
  }

  @JsMethod
  public boolean boxedComp() {
    return Boolean.TRUE == Boolean.TRUE;
  }

  @Test
  public void boxedCompOptimizes() {
    assertFunctionMatches(((MethodsAsProperties) this).getBoxedComp(), "return !0;");
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface MethodsAsProperties {
    @JsProperty
    Object getSimpleComp();

    @JsProperty
    Object getBoxedComp();
  }
}
