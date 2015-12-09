package com.google.j2cl.transpiler.readable.jsfunction;

import jsinterop.annotations.JsMethod;

public class Main {
  public static int fun(MyJsFunctionInterface fn, int a) {
    return fn.foo(a);
  }

  public void test() {
    MyJsFunctionImpl func = new MyJsFunctionImpl();
    // call by java
    func.foo(10);
    // call by js
    callAsFunction(func, 10);
    // pass Javascript function to java.
    fun(createMyJsFunction(), 10);
    // call other instance methods and fields.
    int a = func.field;
    func.bar();
  }

  @JsMethod
  public static native int callAsFunction(Object fn, int arg);

  @JsMethod
  public static native MyJsFunctionInterface createMyJsFunction();
}
