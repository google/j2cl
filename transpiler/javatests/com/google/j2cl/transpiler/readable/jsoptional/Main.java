package com.google.j2cl.transpiler.readable.jsoptional;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;

public class Main {
  @JsMethod
  public void method1(int i1, @JsOptional Double d, @JsOptional Integer i) {}

  @JsMethod
  public void method2(String s1, @JsOptional Double d, Boolean... i) {}

  @JsFunction
  static interface Function {
    Object f1(@JsOptional String i, Object... args);
  }

  @JsConstructor
  public Main(@JsOptional String a) {}

  static class AFunction implements Function {
    @Override
    public Object f1(@JsOptional String i, Object... args) {
      return args[0];
    }
  }
}
