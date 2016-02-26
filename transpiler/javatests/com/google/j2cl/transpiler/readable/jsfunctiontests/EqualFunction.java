package com.google.j2cl.transpiler.readable.jsfunctiontests;

import jsinterop.annotations.JsFunction;

@JsFunction
public interface EqualFunction<T> {
  boolean equal(T first, T second);
}
