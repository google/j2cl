package com.google.j2cl.transpiler.readable.jsfunctiontypeannotation;

import jsinterop.annotations.JsFunction;

public class ImportTypeInJsFunctionMethod {
  @JsFunction
  interface FooFunction {
    void apply(Main main);
  }

  FooFunction fooFunction;
}
