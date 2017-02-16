package com.google.j2cl.transpiler.readable.jsoptional;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

public class Main {
  @JsMethod
  public void method1(int i1, @JsOptional Double d, @JsOptional Integer i) {}

  @JsMethod
  public void method2(String s1, @JsOptional Double d, Boolean... i) {}

  @JsFunction
  interface Function {
    Object f1(@JsOptional String i, Object... args);
  }

  @JsConstructor
  public Main(@JsOptional String a) {}

  static final class AFunction implements Function {
    @Override
    public Object f1(@JsOptional String i, Object... args) {
      return args[0];
    }
  }
}

@JsType
interface I<T> {
  void m(T t, @JsOptional Object o);
}

@JsType
class TemplatedSubtype<T extends String> implements I<T> {
  @Override
  public void m(T t, @JsOptional Object o) {}
}

@JsType
class SpecializedSubtype implements I<String> {
  @Override
  public void m(String t, @JsOptional Object o) {}
}

class NonJsTypeSubtype implements I<String> {
  @Override
  public void m(String t, @JsOptional Object o) {}
}
