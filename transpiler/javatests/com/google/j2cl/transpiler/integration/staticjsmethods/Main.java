package com.google.j2cl.transpiler.integration.staticjsmethods;

import jsinterop.annotations.JsMethod;

public class Main {
  @JsMethod(name = "fun")
  public static int f1(int a) {
    return a + 11;
  }

  @JsMethod
  public static int f2(int a) {
    return a + 22;
  }

  public static void testJsMethodsCalledByJava() {
    assert f1(1) == 12;
    assert f2(1) == 23;
  }

  public static void testJsMethodsCalledByJS() {
    assert callF1(1) == 12;
    assert callF2(1) == 23;
  }

  public static void testJsMethodsCalledByOtherClass() {
    assert OtherClass.callF1(1) == 12;
    assert OtherClass.callF2(1) == 23;
  }

  public static void testNativeJsMethod() {
    assert floor(1.5) == 1;
    assert f3(-1) == 1;
  }

  public static void main(String... args) {
    testJsMethodsCalledByJava();
    testJsMethodsCalledByJS();
    testJsMethodsCalledByOtherClass();
    testNativeJsMethod();
  }

  public static native int callF1(int a);

  public static native int callF2(int a);

  @JsMethod(namespace = "Math")
  public static native int floor(double d);

  @JsMethod(namespace = "Math", name = "abs")
  public static native int f3(int d);
}
