package com.google.j2cl.transpiler.readable.bridgemethoddefault;

import jsinterop.annotations.JsMethod;

public class BridgeMethodDefault {
  interface I<T> {
    void m(T t);
  }

  interface II extends I<String> {
    @Override
    default void m(String s) {}
  }

  interface JJ extends I<Object> {
    @JsMethod
    @Override
    default void m(Object o) {}
  }

  class A implements II {}

  class B implements JJ {}
}
