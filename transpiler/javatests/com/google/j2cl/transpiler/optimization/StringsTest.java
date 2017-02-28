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
class StringsTest {

  @JsMethod
  public boolean stringEqualsString() {
    return "".equals("");
  }

  @JsMethod
  public boolean stringNotEqualsString() {
    return "".equals("asd");
  }

  @Test
  public void simpleEqualsOptimizes() {
    assertFunctionMatches(((MethodsAsProperties) this).getStringEqualsString(), "return !0;");
    assertFunctionMatches(((MethodsAsProperties) this).getStringNotEqualsString(), "return !1;");
  }

  @JsMethod
  public boolean stringSameString() {
    return "" == "";
  }

  @Test
  public void simpleSameOptimizes() {
    assertFunctionMatches(((MethodsAsProperties) this).getStringSameString(), "return !0;");
  }

  private static boolean staticField = "asd".equals("asd");

  @JsMethod
  public boolean stringEqualsStringOnStatic() {
    return staticField;
  }

  @Test
  public void staticFieldEqualsOptimizes() {
    assertFunctionMatches(
        ((MethodsAsProperties) this).getStringEqualsStringOnStatic(), "return !0;");
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface MethodsAsProperties {
    @JsProperty
    Object getStringEqualsString();

    @JsProperty
    Object getStringNotEqualsString();

    @JsProperty
    Object getStringSameString();

    @JsProperty
    Object getStringEqualsStringOnStatic();
  }
}
