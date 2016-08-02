package com.google.j2cl.transpiler.readable.bridgemethoddefault;

import jsinterop.annotations.JsMethod;

interface JJ extends I<Object> {
  @JsMethod
  @Override
  default void m(Object o) {}
}
